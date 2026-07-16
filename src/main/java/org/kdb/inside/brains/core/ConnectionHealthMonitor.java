package org.kdb.inside.brains.core;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import kx.KxConnection;
import kx.c;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Application-level connection health monitoring owned by {@link KdbConnectionManager}:
 * <ul>
 * <li><b>Heartbeat + watchdog:</b> while a connection is idle (no user query in flight), a
 * side-effect-free sync probe ({@code "::"}) is sent periodically. A watchdog force-closes the
 * socket if the probe does not complete within the configured timeout, which unblocks the parked
 * read with an IOException and flows into the normal disconnect handling. This catches connections
 * silently orphaned by network path changes (VPN/ZTNA re-assert after sleep/wake) that the remote
 * never terminates with FIN/RST.</li>
 * <li><b>Auto-reconnect:</b> when a previously established connection dies with an error (heartbeat-,
 * keepalive- or IOException-detected) and the {@code autoReconnect} option is enabled, reconnection
 * is retried with backoff until it succeeds, the attempts cap is reached, the instance is removed,
 * or the user disconnects manually.</li>
 * </ul>
 * Asynchronous connections ({@code async} option) are never actively probed: a sync probe response
 * could be mistaken for the reply a pending async round-trip is waiting for. They still benefit from
 * the tuned TCP keepalive in {@code kx.c#io(Socket)}.
 * <p>
 * Heartbeats are disabled per connection via the {@code heartbeat} instance option; with the option
 * off no task is ever scheduled for the connection and the code paths are identical to upstream.
 * <p>
 * Lifecycle of a monitored entry: created on {@link #connectionOpened}, removed on intentional
 * disconnect ({@link #connectionClosed} with {@code null} error, {@link #reconnectAborted},
 * {@link #connectionRemoved}). Auto-reconnect therefore only ever runs for a connection that was
 * successfully established and has not been manually disconnected since.
 */
public class ConnectionHealthMonitor implements Disposable {
    private static final Logger log = Logger.getInstance(ConnectionHealthMonitor.class);

    static final int[] RECONNECT_DELAYS_SEC = {2, 5, 10, 30};
    static final int MAX_RECONNECT_ATTEMPTS = 20;

    private final DeadConnectionHandler deadConnectionHandler;
    private final ReconnectHandler reconnectHandler;
    private final Executor connectExecutor;

    private final Map<InstanceConnection, MonitoredConnection> monitored = new ConcurrentHashMap<>();

    /**
     * Runs heartbeat ticks and reconnect timers; a tick blocks for up to the heartbeat timeout when
     * the connection is dead, so watchdogs run on their own thread ({@link #watchdogScheduler}) and
     * can never be starved by parked probes.
     */
    private final ScheduledExecutorService heartbeatScheduler;
    private final ScheduledExecutorService watchdogScheduler;

    private volatile boolean disposed = false;

    /**
     * @param deadConnectionHandler invoked (from a heartbeat or watchdog thread) when a probe
     *                              declared the connection dead; receives the probed KxConnection
     *                              so a stale death can be recognised and ignored
     * @param reconnectHandler      performs one reconnect attempt; invoked via {@code connectExecutor}
     * @param connectExecutor       executes reconnect attempts; the production wiring hops to the EDT
     */
    public ConnectionHealthMonitor(DeadConnectionHandler deadConnectionHandler, ReconnectHandler reconnectHandler, Executor connectExecutor) {
        this.deadConnectionHandler = deadConnectionHandler;
        this.reconnectHandler = reconnectHandler;
        this.connectExecutor = connectExecutor;

        final AtomicInteger counter = new AtomicInteger();
        heartbeatScheduler = Executors.newScheduledThreadPool(2, r -> {
            final Thread t = new Thread(r, "kdb-heartbeat-" + counter.getAndIncrement());
            t.setDaemon(true);
            return t;
        });
        watchdogScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            final Thread t = new Thread(r, "kdb-heartbeat-watchdog");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * A connection has been (re-)established. Resets any reconnect bookkeeping and starts the
     * heartbeat when enabled for a synchronous connection.
     */
    public void connectionOpened(InstanceConnection connection, KxConnection kx, InstanceOptions options) {
        if (disposed) {
            return;
        }
        final MonitoredConnection mc = monitored.computeIfAbsent(connection, MonitoredConnection::new);
        synchronized (mc) {
            mc.cancelHeartbeat();
            mc.cancelReconnect();
            mc.reconnectAttempts.set(0);
            mc.kx = kx;
            mc.options = options;

            if (options.isSafeHeartbeatEnabled() && !options.isSafeAsync()) {
                final int interval = options.getSafeHeartbeatIntervalSec();
                mc.heartbeatTask = heartbeatScheduler.scheduleWithFixedDelay(() -> heartbeat(mc), interval, interval, TimeUnit.SECONDS);
                log.debug("Heartbeat started for " + connection.getName() + ": interval " + interval + "s, timeout " + options.getSafeHeartbeatTimeoutMs() + "ms");
            }
        }
    }

    /**
     * A connection has been closed. A {@code null} error means an intentional disconnect and stops
     * all monitoring; an error schedules a reconnect attempt when the option is enabled and the
     * connection had been successfully established before (a monitored entry exists).
     */
    public void connectionClosed(InstanceConnection connection, Exception error, InstanceOptions options) {
        if (error == null || disposed) { // intentional disconnect - stop and forget
            connectionRemoved(connection);
            return;
        }
        final MonitoredConnection mc = monitored.get(connection);
        if (mc == null) {
            return;
        }
        synchronized (mc) {
            mc.cancelHeartbeat();
            mc.kx = null;

            if (options.isSafeAutoReconnect()) {
                scheduleReconnect(mc);
            } else {
                mc.cancelReconnect();
            }
        }
    }

    /**
     * The user asked for a disconnect: stops monitoring entirely, aborting pending reconnect
     * attempts even if the connection is already in the DISCONNECTED state (waiting for the next
     * retry) or a retry task is mid-flight (the generation bump invalidates it).
     */
    public void reconnectAborted(InstanceConnection connection) {
        final MonitoredConnection mc = monitored.remove(connection);
        if (mc != null) {
            synchronized (mc) {
                if (mc.reconnectTask != null) {
                    log.info("Auto-reconnect of " + connection.getName() + " cancelled by manual disconnect");
                }
                mc.cancelHeartbeat();
                mc.cancelReconnect();
            }
        }
    }

    /**
     * The instance has been removed: stop and forget everything about it.
     */
    public void connectionRemoved(InstanceConnection connection) {
        final MonitoredConnection mc = monitored.remove(connection);
        if (mc != null) {
            synchronized (mc) {
                mc.cancelHeartbeat();
                mc.cancelReconnect();
                mc.kx = null;
            }
        }
    }

    @Override
    public void dispose() {
        disposed = true;
        heartbeatScheduler.shutdownNow();
        watchdogScheduler.shutdownNow();
        monitored.clear();
    }

    /**
     * Test hook: runs one heartbeat tick synchronously.
     *
     * @return false if the connection is not monitored
     */
    boolean runHeartbeatNow(InstanceConnection connection) {
        final MonitoredConnection mc = monitored.get(connection);
        if (mc == null) {
            return false;
        }
        heartbeat(mc);
        return true;
    }

    private void heartbeat(MonitoredConnection mc) {
        final InstanceConnection connection = mc.connection;
        final KxConnection kx = mc.kx;
        if (disposed || kx == null || !kx.isConnected() || connection.getState() != InstanceState.CONNECTED) {
            return;
        }
        if (connection.getQuery() != null) { // user query in flight - skip this tick
            log.debug("Heartbeat tick skipped, query in flight: " + connection.getName());
            return;
        }

        final long timeoutMs = mc.options.getSafeHeartbeatTimeoutMs();
        // Death is claimed exactly once, by whoever detects it first: the watchdog (probe exceeded
        // its deadline - even if the response then arrives, the socket is already being closed) or
        // the probe failure path below. ScheduledFuture.cancel() alone cannot arbitrate this: it may
        // report success while the task body is already running.
        final AtomicBoolean deathClaimed = new AtomicBoolean();
        // Armed inside probe(...) right before the write, when the probe is committed: an armed
        // watchdog can therefore never kill a connection whose probe was skipped over a user query.
        final AtomicReference<ScheduledFuture<?>> watchdog = new AtomicReference<>();
        try {
            final boolean probed = kx.probe(
                    () -> connection.getQuery() != null,
                    () -> watchdog.set(watchdogScheduler.schedule(() -> {
                        if (deathClaimed.compareAndSet(false, true)) {
                            final String reason = "Heartbeat probe got no response in " + timeoutMs + "ms";
                            log.warn(reason + " - force-closing the socket of " + connection.getName());
                            kx.close();
                            deadConnectionHandler.connectionDead(connection, kx, new IOException(reason));
                        }
                    }, timeoutMs, TimeUnit.MILLISECONDS)));
            cancelWatchdog(watchdog);
            // if the watchdog claimed the death concurrently (response arrived at ~the deadline), it
            // has already closed the socket and reported the death - nothing more to do here
            if (probed) {
                log.debug("Heartbeat probe ok: " + connection.getName());
            } else {
                log.debug("Heartbeat probe skipped, query in flight: " + connection.getName());
            }
        } catch (c.KException ex) {
            // the remote evaluated the probe and responded, even if with an error - the path is alive
            cancelWatchdog(watchdog);
            log.debug("Heartbeat probe of " + connection.getName() + " got error response: " + ex.getMessage());
        } catch (Throwable ex) {
            cancelWatchdog(watchdog);
            if (deathClaimed.compareAndSet(false, true)) {
                final String reason = "Heartbeat probe failed: " + ex.getMessage();
                log.warn(reason + " - closing connection " + connection.getName(), ex);
                deadConnectionHandler.connectionDead(connection, kx, new IOException(reason, ex));
            }
            // else: the watchdog claimed the death and closed the socket; this exception is the
            // consequence of that close and has already been handled
        }
    }

    private static void cancelWatchdog(AtomicReference<ScheduledFuture<?>> watchdog) {
        final ScheduledFuture<?> task = watchdog.get();
        if (task != null) {
            task.cancel(false);
        }
    }

    /**
     * Caller must hold the {@code mc} lock. The scheduled task and its EDT hop both re-validate the
     * reconnect generation, so {@link MonitoredConnection#cancelReconnect()} (which bumps it)
     * reliably aborts a retry even when the task has already left the scheduler queue.
     */
    private void scheduleReconnect(MonitoredConnection mc) {
        final int attempt = mc.reconnectAttempts.getAndIncrement();
        if (attempt >= MAX_RECONNECT_ATTEMPTS) {
            log.warn("Auto-reconnect of " + mc.connection.getName() + " gave up after " + MAX_RECONNECT_ATTEMPTS + " failed attempts");
            return;
        }
        final int delay = RECONNECT_DELAYS_SEC[Math.min(attempt, RECONNECT_DELAYS_SEC.length - 1)];
        log.info("Auto-reconnect attempt " + (attempt + 1) + "/" + MAX_RECONNECT_ATTEMPTS + " of " + mc.connection.getName() + " in " + delay + "s");

        mc.cancelReconnect();
        final int generation = mc.reconnectGeneration;
        mc.reconnectTask = heartbeatScheduler.schedule(() -> {
            synchronized (mc) {
                if (generation != mc.reconnectGeneration) {
                    return; // aborted/superseded while queued
                }
                mc.reconnectTask = null;
            }
            if (disposed || !monitored.containsKey(mc.connection)) {
                return;
            }
            connectExecutor.execute(() -> {
                synchronized (mc) {
                    if (generation != mc.reconnectGeneration) {
                        return; // aborted between the timer firing and the EDT hop
                    }
                }
                if (!disposed && monitored.containsKey(mc.connection) && mc.connection.getState() == InstanceState.DISCONNECTED) {
                    reconnectHandler.reconnect(mc.connection);
                }
            });
        }, delay, TimeUnit.SECONDS);
    }

    /**
     * Invoked when a heartbeat declared a connection dead; the implementation is expected to close
     * the connection with the given reason through the usual state/listener mechanism, ignoring the
     * report when {@code kx} is no longer the connection's current transport (a stale probe).
     */
    @FunctionalInterface
    public interface DeadConnectionHandler {
        void connectionDead(InstanceConnection connection, KxConnection kx, IOException reason);
    }

    /**
     * Performs one reconnect attempt for a connection that died with an error.
     */
    @FunctionalInterface
    public interface ReconnectHandler {
        void reconnect(InstanceConnection connection);
    }

    private static class MonitoredConnection {
        final InstanceConnection connection;
        final AtomicInteger reconnectAttempts = new AtomicInteger();

        volatile KxConnection kx;
        volatile InstanceOptions options;

        ScheduledFuture<?> heartbeatTask;
        ScheduledFuture<?> reconnectTask;
        // bumped on every cancel: a retry task (even one already running) aborts when its captured
        // generation no longer matches. Guarded by the mc lock.
        int reconnectGeneration;

        MonitoredConnection(InstanceConnection connection) {
            this.connection = connection;
        }

        void cancelHeartbeat() {
            if (heartbeatTask != null) {
                heartbeatTask.cancel(false);
                heartbeatTask = null;
            }
        }

        void cancelReconnect() {
            reconnectGeneration++;
            if (reconnectTask != null) {
                reconnectTask.cancel(false);
                reconnectTask = null;
            }
        }
    }
}
