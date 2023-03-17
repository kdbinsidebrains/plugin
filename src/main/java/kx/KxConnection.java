package kx;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;

public class KxConnection extends c implements Closeable {
    private final int msgType;

    public static final TimeZone UTC_TIMEZONE = TimeZone.getTimeZone("UTC");

    public KxConnection(String host, int port, boolean async, boolean tls, boolean zip) throws IOException {
        tz = UTC_TIMEZONE;
        setEncoding("UTF-8");

        this.zip = zip;
        this.msgType = async ? 0 : 1;

        // We have to split original constructor into socket creating and authentification to be able to cancel
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