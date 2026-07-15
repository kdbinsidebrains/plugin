# Fork notes — dead-connection detection + auto-reconnect

Private internal fork of [kdbinsidebrains/plugin](https://github.com/kdbinsidebrains/plugin),
based on upstream `7.0.1` (commit `d50fd32`), branch `feature/connection-heartbeat`.

## Why

macOS laptop + corporate ZTNA (Jamf Trust) tunnel + plugin-owned TCP connections directly to
remote kdb+ instances. Every morning the Mac wakes from sleep, the network path re-asserts and
silently orphans the long-lived TCP connection crossing the ZTNA gateway. The remote peer never
sends FIN/RST, so nothing detects the death: the plugin's reader threads park forever in
`DataInputStream.readFully` (no SO_TIMEOUT) and the only recourse was restarting IntelliJ.
Jamf engineering's recommendation: enable TCP keepalive on the kdb socket with an interval
below the gateway's idle timeout.

## What changed (three layers)

### 1. Feature B — tuned TCP keepalive (`src/main/java/kx/c.java`)

`io(Socket)` already called `setKeepAlive(true)`, but that uses OS default timings — on macOS
the first probe is sent after **2 hours** idle (`net.inet.tcp.keepidle`), i.e. effectively never.
`tuneKeepAlive(Socket)` now sets, best effort, via `jdk.net.ExtendedSocketOptions`:

| Option | Value | Meaning |
|---|---|---|
| `TCP_KEEPIDLE` | 30 s | idle time before the first probe |
| `TCP_KEEPINTERVAL` | 10 s | gap between unanswered probes |
| `TCP_KEEPCOUNT` | 3 | unanswered probes before the kernel kills the connection |

A dead connection is declared by the kernel within ~60 s; the parked read throws `IOException`
into the existing disconnect handling (instance turns red, one double-click to reconnect).
Unsupported platforms silently keep OS defaults. This layer is passive, always on, and covers
*all* connections including async ones and in-flight user queries.

### 2. Feature A — heartbeat + stalled-read watchdog

- `kx/KxConnection.probe(abort, watchdogArm)` — side-effect-free sync round-trip (`"::"`).
  The whole write+read runs under **both** stream locks (`o` then `i`), so it can never
  interleave with `query(...)`; `abort` is re-checked after the locks are held so a user query
  submitted concurrently always wins and the probe skips. Non-response messages received while
  waiting are consumed with the same bookkeeping as `query(...)`.
- `core/ConnectionHealthMonitor` (new, owned by `KdbConnectionManager`) — schedules a probe per
  idle connection every `heartbeatInterval` s on a shared 2-thread daemon scheduler
  (`kdb-heartbeat-%d`). A watchdog scheduled on a **dedicated** thread
  (`kdb-heartbeat-watchdog`) force-closes the socket when a probe gets no response within
  `heartbeatTimeout` ms — closing unblocks `readFully` with an `IOException` that flows into
  the normal error handling ("instance goes red"). The watchdog is armed *inside* the probe,
  right before the write, so it can never kill a connection whose probe was skipped.
  The dedicated watchdog thread deviates from the original "one scheduler" sketch on purpose:
  probes parked on a dead socket block their pool thread, and watchdogs must not be starvable
  by the very probes they guard.
- Async connections (`async` option) are **never** probed: with `msgType=0` a probe response
  could be consumed by a pending async round-trip. They rely on layer 1.
- Stalled *user* queries are intentionally not watchdogged in v1 (legitimate long queries are
  indistinguishable from stalls at this level); layer 1 covers that case at the kernel level.
- Heartbeat off ⇒ no task is ever scheduled for the connection; code paths identical to upstream.

### 3. Feature C — auto-reconnect (default off)

When a **previously established** connection dies with an error (heartbeat-, keepalive- or
IOException-detected) and `autoReconnect` is on: reconnect with backoff 2 s → 5 s → 10 s → 30 s
(capped, max 20 attempts). Cancelled by manual disconnect, instance removal, plugin disposal or
success. A connection whose *first* connect fails is not retried (bad host/credentials would
loop forever). Reuses the existing `connect()` path, so all listeners/notifications behave as
for a manual reconnect.

### Options plumbing / UI

New per-instance options following the existing builder + XML + `resolve()` inheritance pattern
(scope-level default, instance-level override), editable in
`Settings → Languages & Frameworks → KDB+ Q → Connections → Connection Options`
and in the scope/instance editors:

| Option (XML attribute) | UI label | Default |
|---|---|---|
| `heartbeat` | Health check is enabled | `true` |
| `heartbeatInterval` | Health-check interval, s | `30` (min 5) |
| `heartbeatTimeout` | Health-check timeout, ms | `5000` (min 500) |
| `autoReconnect` | Reconnect automatically | `false` |

Keepalive tuning values (layer 1) are hardcoded constants in `kx.c` for now.

### Build

- `settings.gradle`: added the foojay toolchain resolver so Gradle can auto-provision the JDK 21
  toolchain the build requires.
- `version.properties`: version bumped to `999.701.0` (999 = fork marker, 701 = upstream 7.0.1) so the
  Marketplace can never auto-update over this fork. Keep the `999.` prefix when rebasing.

## Files touched

```
settings.gradle                                              build: toolchain resolver
version.properties, CHANGELOG.md                             fork version
src/main/java/kx/c.java                                      Feature B (keepalive block only)
src/main/java/kx/KxConnection.java                           probe()
src/main/java/org/kdb/inside/brains/core/ConnectionHealthMonitor.java   new
src/main/java/org/kdb/inside/brains/core/KdbConnectionManager.java      monitor wiring
src/main/java/org/kdb/inside/brains/core/InstanceOptions.java           4 new options
src/main/java/org/kdb/inside/brains/core/InstanceOptionsPanel.java      4 new option rows
src/test/java/org/kdb/inside/brains/core/ConnectionHealthMonitorTest.java  new
src/test/java/org/kdb/inside/brains/core/InstanceOptionsTest.java          extended
```

## How to rebase onto upstream

```bash
git remote add upstream https://github.com/kdbinsidebrains/plugin.git   # once
git fetch upstream
git rebase upstream/main feature/connection-heartbeat
```

The diff footprint is deliberately small: one block in `kx/c.java`, one method in
`KxConnection`, one new class, a handful of hook lines in `KdbConnectionManager`, and the
mechanical options/UI additions. The likeliest conflicts are in `InstanceOptions` /
`InstanceOptionsPanel` if upstream adds options of its own — resolve by re-applying the four
option groups. Re-apply the `999.` version prefix after rebasing.

## Test evidence

- `./gradlew test` — full suite passes (including upstream tests).
- `ConnectionHealthMonitorTest` runs the real IPC byte flow against a fake kdb+ server
  (handshake + canned responses): probe round-trip on a live connection, error response treated
  as alive, watchdog kill on a stalled probe (~500 ms), probe skipped while a user query is in
  flight, reconnect backoff timing, reconnect cancellation on manual disconnect / intentional
  close / disabled option, no retry for never-established connections.
- `InstanceOptionsTest` extended for the new options: builder validation, URL-parameter and XML
  round-trips, `safe()` defaults.

Manual validation checklist (on the real laptop):

1. Install zip, connect to a remote q instance, `kill -STOP` the remote q process (stalls
   traffic without FIN) → instance should turn red within ~35 s (heartbeat) / ~60 s (keepalive
   alone, heartbeat off).
2. Leave IntelliJ connected overnight, let the Mac sleep → next morning the instance shows
   disconnected (not hung) within a minute of wake; reconnect in one click, or automatically
   with `Reconnect automatically` enabled.
3. Hammer queries in a loop with heartbeat on → results stay correct, no interleaving.
