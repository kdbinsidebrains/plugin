package kx;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class KxConnection extends c implements Closeable {
    private final int msgType;

    public static final TimeZone UTC_TIMEZONE = TimeZone.getTimeZone("UTC");

    public KxConnection(String host, int port, boolean async, boolean tls, boolean zip, String encoding) throws IOException {
        this.tz = UTC_TIMEZONE;
        this.zip = zip;
        this.msgType = async ? 0 : 1;
        this.encoding = encoding != null ? encoding : "UTF-8";

        // We have to split the original constructor into socket creating and authentication to be able to cancel
        // authentication - it could take too long if the instance is busy.
        s = new Socket(host.isBlank() ? "localhost" : host, port);
        if (tls) {
            try {
                s = ((SSLSocketFactory) SSLSocketFactory.getDefault()).createSocket(s, host, port, true);
                ((SSLSocket) s).startHandshake();
            } catch (Exception e) {
                s.close();
                throw e;
            }
        }
        io(s);
    }

    public Object query(Object x, CancellationValidator cancellation, ResponseValidator responseValidator, Consumer<QueryPhase> phaseConsumer) throws IOException, KException, CancellationException {
        if (o == null || i == null) {
            throw new IOException("Connection lost");
        }
        synchronized (o) {
            cancellation.checkCancelled();
            phaseConsumer.accept(QueryPhase.ENCODING);
            byte[] buffer = serialize(msgType, x, zip);

            cancellation.checkCancelled();
            phaseConsumer.accept(QueryPhase.SENDING);
            o.write(buffer, 0, buffer.length);
        }

        phaseConsumer.accept(QueryPhase.WAITING);
        synchronized (i) {
            i.readFully(b = new byte[8]); // read the msg header
            a = b[0] == 1;  // endianness of the msg
            if (b[1] == 1) { // msg types are 0 - async, 1 - sync, 2 - response
                sync++;   // an incoming sync message means the remote will expect a response message
            }
            j = 4;

            final int size = ri();
            final int readCount = size - 8;

            phaseConsumer.accept(QueryPhase.RECEIVING);
            try {
                cancellation.checkCancelled();
                responseValidator.checkMessageSize(size);
            } catch (CancellationException ex) {
                i.skipBytes(readCount);
                throw ex;
            }

            b = Arrays.copyOf(b, size);
            i.readFully(b, 8, readCount); // read the incoming message in full

            cancellation.checkCancelled();
            phaseConsumer.accept(QueryPhase.DECODING);
            return deserialize(b);
        }
    }

    /**
     * Sends a lightweight, side-effect-free sync round-trip ({@code "::"}) to validate that the
     * connection is alive end-to-end.
     * <p>
     * The whole write+read cycle is performed under both the {@code o} and {@code i} locks so it can
     * never interleave with {@link #query(Object, CancellationValidator, ResponseValidator, Consumer)}:
     * a user query started while the probe is in flight blocks on the {@code o} lock until the probe
     * response has been fully consumed.
     * <p>
     * {@code abort} is re-checked after the locks are acquired: if a user query has been submitted in
     * the meantime (its write may already be on the wire), probing now could consume the response
     * destined for that query, so the probe is skipped. The mutual exclusion on {@code o} guarantees
     * that a positive "no query in flight" answer inside the lock is authoritative.
     * <p>
     * {@code watchdogArm} is invoked right before the probe bytes are written - the caller is expected
     * to schedule a task that force-closes this connection if the probe does not complete in time,
     * which unblocks the {@code readFully} below with an IOException on a dead connection.
     * <p>
     * Any non-response messages (async pushes, incoming sync requests) received while waiting are
     * consumed and discarded with the same bookkeeping as {@code query(...)}.
     *
     * @param abort       returns true if the probe must be skipped (a user query is in flight)
     * @param watchdogArm invoked once probing is committed, right before the write
     * @return true if the probe completed the round-trip; false if it was skipped
     * @throws IOException if the connection is broken (or was force-closed by the watchdog)
     * @throws KException  if the remote responded with an error - the connection is alive
     */
    public boolean probe(BooleanSupplier abort, Runnable watchdogArm) throws IOException, KException {
        final OutputStream out = o;
        final DataInputStream in = i;
        if (out == null || in == null) {
            throw new IOException("Connection lost");
        }
        synchronized (out) {
            if (abort.getAsBoolean()) {
                return false;
            }
            synchronized (in) {
                watchdogArm.run();

                final byte[] buffer = serialize(1, "::", false);
                out.write(buffer, 0, buffer.length);

                while (true) {
                    in.readFully(b = new byte[8]); // read the msg header
                    a = b[0] == 1;  // endianness of the msg
                    final byte msgType = b[1];
                    if (msgType == 1) { // an incoming sync message means the remote will expect a response message
                        sync++;
                    }
                    j = 4;

                    final int size = ri();
                    b = Arrays.copyOf(b, size);
                    in.readFully(b, 8, size - 8); // read the incoming message in full

                    if (msgType == 2) { // the probe response; anything else is discarded
                        deserialize(b);
                        return true;
                    }
                }
            }
        }
    }

    public void authenticate(String credentials) throws IOException, KException {
        J = 0;
        B = new byte[2 + ns(credentials)];
        w(credentials + "\3");
        o.write(B);
        if (1 != i.read(B, 0, 1)) {
            close();
            throw new KException("access");
        }
        vt = Math.min(B[0], 3);
    }

    @Override
    public void close() {
        if (null != s) {
            try {
                s.close();
            } catch (IOException ignore) {
                //
            }
            s = null;
        }
        if (null != i) {
            try {
                i.close();
            } catch (IOException ignore) {
                //
            }
            i = null;
        }
        if (null != o) {
            try {
                o.close();
            } catch (IOException ignore) {
                //
            }
            o = null;
        }
    }

    public boolean isConnected() {
        return s != null;
    }

    public static boolean isNull(Object o) {
        if (o == null) {
            return true;
        }
        for (Object o1 : NULL) {
            if (Objects.equals(o, o1)) {
                return true;
            }
        }
        return false;
    }
}