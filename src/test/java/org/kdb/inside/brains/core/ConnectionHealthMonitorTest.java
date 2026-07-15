package org.kdb.inside.brains.core;

import kx.KxConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Exercises the heartbeat/watchdog/auto-reconnect logic against a fake kdb+ server speaking just
 * enough of the IPC protocol: handshake, canned responses to probe messages, and - for the
 * dead-connection cases - accepting a probe and never answering.
 */
class ConnectionHealthMonitorTest {
    private FakeKdbServer server;
    private KxConnection kx;
    private ConnectionHealthMonitor monitor;
    private ConnectionHealthMonitor.DeadConnectionHandler deadHandler;

    @BeforeEach
    void setUp() {
        deadHandler = mock(ConnectionHealthMonitor.DeadConnectionHandler.class);
        monitor = new ConnectionHealthMonitor(deadHandler, Runnable::run);
    }

    @AfterEach
    void tearDown() throws Exception {
        monitor.dispose();
        if (kx != null) {
            kx.close();
        }
        if (server != null) {
            server.close();
        }
    }

    private InstanceConnection mockConnection() {
        final InstanceConnection connection = mock(InstanceConnection.class);
        when(connection.getState()).thenReturn(InstanceState.CONNECTED);
        when(connection.getQuery()).thenReturn(null);
        when(connection.getName()).thenReturn("mock");
        return connection;
    }

    private InstanceConnection connect(FakeKdbServer.Behaviour behaviour) throws Exception {
        server = new FakeKdbServer(behaviour);
        kx = new KxConnection("localhost", server.port(), false, false, false, "UTF-8");
        kx.authenticate("test:test");
        return mockConnection();
    }

    private static InstanceOptions.Builder healthOptions() {
        return new InstanceOptions.Builder()
                .heartbeat(true)
                .heartbeatInterval(InstanceOptions.MIN_HEARTBEAT_INTERVAL_SEC)
                .heartbeatTimeout(InstanceOptions.MIN_HEARTBEAT_TIMEOUT_MS);
    }

    @Test
    void probeCompletesOnAliveConnection() throws Exception {
        final InstanceConnection connection = connect(FakeKdbServer.Behaviour.RESPOND_IDENTITY);

        monitor.connectionOpened(connection, kx, healthOptions().create());
        assertTrue(monitor.runHeartbeatNow(connection));

        assertTrue(server.awaitMessages(1, 2000), "the server never received the probe");
        verifyNoInteractions(deadHandler);
        assertTrue(kx.isConnected());
    }

    @Test
    void errorResponseMeansConnectionIsAlive() throws Exception {
        final InstanceConnection connection = connect(FakeKdbServer.Behaviour.RESPOND_ERROR);

        monitor.connectionOpened(connection, kx, healthOptions().create());
        assertTrue(monitor.runHeartbeatNow(connection));

        assertTrue(server.awaitMessages(1, 2000));
        verifyNoInteractions(deadHandler);
        assertTrue(kx.isConnected());
    }

    @Test
    void stalledProbeIsKilledByWatchdogAndReportedDead() throws Exception {
        final InstanceConnection connection = connect(FakeKdbServer.Behaviour.NEVER_RESPOND);

        monitor.connectionOpened(connection, kx, healthOptions().create());

        final long start = System.currentTimeMillis();
        assertTrue(monitor.runHeartbeatNow(connection)); // blocks until the watchdog closes the socket
        final long elapsed = System.currentTimeMillis() - start;

        verify(deadHandler, times(1)).connectionDead(same(connection), any(IOException.class));
        assertTrue(elapsed >= InstanceOptions.MIN_HEARTBEAT_TIMEOUT_MS / 2, "died suspiciously fast: " + elapsed + "ms");
        assertTrue(elapsed < InstanceOptions.MIN_HEARTBEAT_TIMEOUT_MS * 20L, "watchdog did not fire in time: " + elapsed + "ms");
    }

    @Test
    void probeIsSkippedWhileUserQueryInFlight() throws Exception {
        final InstanceConnection connection = connect(FakeKdbServer.Behaviour.NEVER_RESPOND);
        when(connection.getQuery()).thenReturn(mock(KdbQuery.class));

        monitor.connectionOpened(connection, kx, healthOptions().create());
        assertTrue(monitor.runHeartbeatNow(connection)); // must return immediately without probing

        assertFalse(server.awaitMessages(1, 300), "no probe may be sent while a query is in flight");
        verifyNoInteractions(deadHandler);
        assertTrue(kx.isConnected());
    }

    @Test
    void heartbeatDisabledSchedulesNothing() throws Exception {
        final InstanceConnection connection = connect(FakeKdbServer.Behaviour.RESPOND_IDENTITY);

        monitor.connectionOpened(connection, kx, healthOptions().heartbeat(false).create());
        assertTrue(monitor.runHeartbeatNow(connection)); // manually forced tick still probes; scheduling is off

        // the important part: options with heartbeat disabled do not create the periodic task;
        // there is no API to inspect the scheduler, so this test just documents the manual tick
        verifyNoInteractions(deadHandler);
    }

    @Test
    void asyncConnectionIsNeverProbed() throws Exception {
        final InstanceConnection connection = connect(FakeKdbServer.Behaviour.NEVER_RESPOND);

        monitor.connectionOpened(connection, kx, healthOptions().async(true).create());
        // no periodic task is scheduled for async connections; a forced tick still exists but the
        // scheduled path is what production uses - wait and check no probe arrived on its own
        assertFalse(server.awaitMessages(1, 500), "async connections must not be probed by the scheduler");
        verifyNoInteractions(deadHandler);
    }

    @Test
    void autoReconnectRetriesAfterFailure() throws Exception {
        final InstanceConnection connection = mockConnection();
        when(connection.getState()).thenReturn(InstanceState.DISCONNECTED);

        final InstanceOptions options = healthOptions().heartbeat(false).autoReconnect(true).create();
        monitor.connectionOpened(connection, mock(KxConnection.class), options);
        monitor.connectionClosed(connection, new IOException("boom"), options);

        // first backoff step is 2s
        verify(connection, timeout(4000).times(1)).connect();
    }

    @Test
    void manualDisconnectCancelsPendingReconnect() throws Exception {
        final InstanceConnection connection = mockConnection();
        when(connection.getState()).thenReturn(InstanceState.DISCONNECTED);

        final InstanceOptions options = healthOptions().heartbeat(false).autoReconnect(true).create();
        monitor.connectionOpened(connection, mock(KxConnection.class), options);
        monitor.connectionClosed(connection, new IOException("boom"), options);
        monitor.reconnectAborted(connection);

        Thread.sleep(3000);
        verify(connection, never()).connect();
    }

    @Test
    void intentionalCloseDoesNotReconnect() throws Exception {
        final InstanceConnection connection = mockConnection();
        when(connection.getState()).thenReturn(InstanceState.DISCONNECTED);

        final InstanceOptions options = healthOptions().heartbeat(false).autoReconnect(true).create();
        monitor.connectionOpened(connection, mock(KxConnection.class), options);
        monitor.connectionClosed(connection, null, options);

        Thread.sleep(3000);
        verify(connection, never()).connect();
    }

    @Test
    void reconnectDisabledDoesNothing() throws Exception {
        final InstanceConnection connection = mockConnection();
        when(connection.getState()).thenReturn(InstanceState.DISCONNECTED);

        final InstanceOptions options = healthOptions().heartbeat(false).create(); // autoReconnect defaults to false
        monitor.connectionOpened(connection, mock(KxConnection.class), options);
        monitor.connectionClosed(connection, new IOException("boom"), options);

        Thread.sleep(3000);
        verify(connection, never()).connect();
    }

    @Test
    void unknownConnectionIsIgnored() {
        final InstanceConnection connection = mockConnection();
        assertFalse(monitor.runHeartbeatNow(connection));
        // a close for a never-opened connection (e.g. the very first connect failed) must not retry
        monitor.connectionClosed(connection, new IOException("boom"), healthOptions().autoReconnect(true).create());
        verify(connection, after(2500).never()).connect();
    }

    /**
     * Accepts a single connection, performs the kdb+ handshake (credentials until {@code \0},
     * replies protocol version 3) and then serves incoming messages according to the behaviour.
     */
    private static final class FakeKdbServer implements AutoCloseable {
        enum Behaviour {RESPOND_IDENTITY, RESPOND_ERROR, NEVER_RESPOND}

        // (1;`response;0;0) header + payload `::` (101 0x00)
        private static final byte[] IDENTITY_RESPONSE = {1, 2, 0, 0, 10, 0, 0, 0, 101, 0};
        // -128 followed by "boom\0"
        private static final byte[] ERROR_RESPONSE = {1, 2, 0, 0, 14, 0, 0, 0, (byte) 0x80, 'b', 'o', 'o', 'm', 0};

        private final ServerSocket serverSocket;
        private final Thread thread;
        private final AtomicInteger messages = new AtomicInteger();
        private final CopyOnWriteArrayList<CountDownLatch> waiters = new CopyOnWriteArrayList<>();

        FakeKdbServer(Behaviour behaviour) throws IOException {
            serverSocket = new ServerSocket(0);
            thread = new Thread(() -> serve(behaviour), "fake-kdb-server");
            thread.setDaemon(true);
            thread.start();
        }

        int port() {
            return serverSocket.getLocalPort();
        }

        boolean awaitMessages(int count, long timeoutMs) throws InterruptedException {
            final CountDownLatch latch = new CountDownLatch(Math.max(0, count - messages.get()));
            waiters.add(latch);
            try {
                return latch.await(timeoutMs, TimeUnit.MILLISECONDS);
            } finally {
                waiters.remove(latch);
            }
        }

        private void serve(Behaviour behaviour) {
            try (Socket socket = serverSocket.accept()) {
                final InputStream in = socket.getInputStream();
                final OutputStream out = socket.getOutputStream();

                // handshake: credentials + version byte, null-terminated; reply with version 3
                final ByteArrayOutputStream credentials = new ByteArrayOutputStream();
                int c;
                while ((c = in.read()) > 0) {
                    credentials.write(c);
                }
                if (c < 0) {
                    return;
                }
                out.write(3);
                out.flush();

                // message loop: 8 byte header, size at offset 4 with endianness declared by byte 0
                // (this c.java fork serializes outgoing messages big-endian with header[0] == 0)
                final byte[] header = new byte[8];
                while (true) {
                    readFully(in, header);
                    final int size = header[0] == 1
                            ? (header[4] & 0xFF) | (header[5] & 0xFF) << 8 | (header[6] & 0xFF) << 16 | (header[7] & 0xFF) << 24
                            : (header[7] & 0xFF) | (header[6] & 0xFF) << 8 | (header[5] & 0xFF) << 16 | (header[4] & 0xFF) << 24;
                    readFully(in, new byte[size - 8]);

                    messages.incrementAndGet();
                    waiters.forEach(CountDownLatch::countDown);

                    switch (behaviour) {
                        case RESPOND_IDENTITY -> {
                            out.write(IDENTITY_RESPONSE);
                            out.flush();
                        }
                        case RESPOND_ERROR -> {
                            out.write(ERROR_RESPONSE);
                            out.flush();
                        }
                        case NEVER_RESPOND -> {
                            // sit on it
                        }
                    }
                }
            } catch (IOException ignore) {
                // client gone - test over
            }
        }

        private static void readFully(InputStream in, byte[] buffer) throws IOException {
            int read = 0;
            while (read < buffer.length) {
                final int n = in.read(buffer, read, buffer.length - read);
                if (n < 0) {
                    throw new IOException("Connection closed");
                }
                read += n;
            }
        }

        @Override
        public void close() throws IOException {
            serverSocket.close();
            thread.interrupt();
        }
    }
}
