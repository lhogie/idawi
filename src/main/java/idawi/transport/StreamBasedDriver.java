package idawi.transport;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.stream.Stream;

import idawi.Component;
import idawi.Idawi;
import idawi.messaging.Message;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import toools.util.Conversion;

public abstract class StreamBasedDriver extends TransportService implements Broadcastable {

	public StreamBasedDriver(Component c) {
		super(c);

	}

	private void newThread(InputStream in) {
		Idawi.agenda.threadPool.submit(() -> {
			try {
				inputStreamDecoder(in, bytes -> callback(bytes));

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
			);
	}

	public void threadAllocator() {
		inputStreams().forEach(in -> newThread(in));
	}

	private void callback(byte[] bytes) {
		System.out.println("yes");

		var msgBytes = Arrays.copyOf(bytes, bytes.length - 4);
		int hashCode = ByteBuffer.wrap(bytes, bytes.length - 4, 4).getInt();

		if (Arrays.hashCode(msgBytes) == hashCode) {
			Message testBytes=(Message) serializer.fromBytes(bytes);
			System.out.println(testBytes);
			processIncomingMessage((Message) serializer.fromBytes(bytes));
		} else {
			System.err.println("garbage");
		}
	}

	public static final byte[] marker = "fgmfkdjgvhdfkghksfjhfdsj".getBytes();

	protected abstract Stream<InputStream> inputStreams();

	protected abstract Stream<OutputStream> outputStreams();

	@Override
	protected void multicast(byte[] msgBytes, Collection<Link> outLinks) {
		bcast(msgBytes);
	}

	@Override
	public void bcast(byte[] msgBytes) {
		outputStreams().forEach(os -> {
			try {
				var b = new ByteArrayOutputStream();
				b.write(msgBytes);
				b.write(Conversion.intToBytes(Arrays.hashCode(msgBytes)));
				b.write(marker);
				os.write(b.toByteArray());
				System.out.println("writing done");

			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}

	public static void inputStreamDecoder(InputStream in, Consumer<byte[]> callback) {
		try {

			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			while (true) {


				int i = in.read();
				bytes.write( (byte)i);

				if ((bytes.size()>=marker.length) && endsBy(marker, bytes)) {
					callback.accept(Arrays.copyOf(bytes.toByteArray(), bytes.size() - marker.length));
					bytes.reset();
					bytes.write( (byte)i);

				}
			}

		} catch (IOException err) {
			System.err.println("I/O error reading stream");
		} 
	}

	private static boolean endsBy(byte[] marker, ByteArrayOutputStream l) throws UnsupportedEncodingException {

		var buf = l.toByteArray();


		return Arrays.equals(buf, l.size() - marker.length, l.size(), marker, 0, marker.length);
	}
}
