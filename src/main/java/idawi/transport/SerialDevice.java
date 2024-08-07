package idawi.transport;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;

import idawi.Component;
import idawi.messaging.Message;
import toools.util.Conversion;

public class SerialDevice extends TransportService implements Broadcastable {
    private InputStream inputStream;
    private OutputStream outputStream;

    public SerialDevice(Component c, InputStream inputStream, OutputStream outputStream) {
        super(c);
        setInputStream(inputStream);
        setOutputStream(outputStream);
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    public void setOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    protected void callback(byte[] bytes) {
        System.out.println("yes");

        var msgBytes = Arrays.copyOf(bytes, bytes.length - 4);
        int hashCode = ByteBuffer.wrap(bytes, bytes.length - 4, 4).getInt();

        if (Arrays.hashCode(msgBytes) == hashCode) {
            Message testBytes = (Message) serializer.fromBytes(bytes);
            System.out.println(testBytes);
            processIncomingMessage((Message) serializer.fromBytes(bytes));
        } else {
            System.err.println("garbage");
        }
    }

    public static final byte[] marker = "fgmfkdjgvhdfkghksfjhfdsj".getBytes();

    @Override
    protected void multicast(byte[] msgBytes, Collection<Link> outLinks) {
        bcast(msgBytes);
    }

    @Override
    public void bcast(byte[] msgBytes) {

        OutputStream os = getOutputStream();
        try {
            var b = new ByteArrayOutputStream();
            b.write(msgBytes);
            b.write(Conversion.intToBytes(Arrays.hashCode(msgBytes)));
            b.write(marker);
            System.out.println(b.toByteArray());
            System.out.println((Arrays.hashCode(msgBytes)));

            os.write(b.toByteArray());
            System.out.println("writing done");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void inputStreamDecoder() {
        try {

            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            while (true) {
                int i = getInputStream().read();
                bytes.write((byte) i);
                if ((bytes.size() >= marker.length) && endsBy(marker, bytes)) {
                    System.out.println("nice");
                    callback(Arrays.copyOf(bytes.toByteArray(), bytes.size() - marker.length));
                    bytes.reset();

                }
            }

        } catch (IOException err) {
            System.err.println("I/O error reading stream");
        }
    }

    protected static boolean endsBy(byte[] marker, ByteArrayOutputStream l) throws UnsupportedEncodingException {
        var buf = l.toByteArray();
        return Arrays.equals(buf, l.size() - marker.length, l.size(), marker, 0, marker.length);
    }

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getName'");
    }

    @Override
    public void dispose(Link l) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'dispose'");
    }

    @Override
    public double latency() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'latency'");
    }
}
