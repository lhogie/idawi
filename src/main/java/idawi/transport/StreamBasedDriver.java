package idawi.transport;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
		Idawi.agenda.threadPool.submit(() -> inputStreamDecoder(in, bytes -> callback(bytes)));
	}

	public void threadAllocator() {
		inputStreams().forEach(in -> newThread(in));
	}

	private void callback(byte[] bytes) {
		var msgBytes = Arrays.copyOf(bytes, bytes.length - 4);
		int hashCode = ByteBuffer.wrap(bytes, bytes.length - 4, 4).getInt();

		if (Arrays.hashCode(msgBytes) == hashCode) {
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
				System.out.println(b.toByteArray());
				System.out.println((Arrays.hashCode(msgBytes)));

				os.write(b.toByteArray());
				System.out.println("writing done");
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}

	public static void inputStreamDecoder(InputStream in, Consumer<byte[]> callback) {
		try {
			var bytes = new ByteArrayList();
			while (true) {
				int i = in.read();

				if (i == -1) {
					return;
				}

				bytes.add((byte) i);

				if (endsBy(marker, bytes)) {

					callback.accept(Arrays.copyOf(bytes.elements(), bytes.size() - marker.length));
					bytes.clear();
					bytes.add((byte) i);

				}
			}

		} catch (IOException err) {
			System.err.println("I/O error reading stream");
		}
	}

	private static boolean endsBy(byte[] marker, ByteArrayList l) {
		var buf = l.elements();
		return Arrays.equals(buf, l.size() - marker.length, l.size(), marker, 0, marker.length);
	}
}
