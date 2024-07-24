package idawi.transport;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.stream.Stream;

import idawi.Component;
import idawi.messaging.Message;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import toools.util.Conversion;

public abstract class InputStreamBasedDriver extends TransportService implements Broadcastable {

	public InputStreamBasedDriver(Component c) {
		super(c);
		// TODO Auto-generated constructor stub
	}

	public static final byte[] marker = ByteBuffer.wrap(new byte[8]).putLong(939196893501413829L).array();

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
				os.write(msgBytes);
				os.write(Conversion.intToBytes(Arrays.hashCode(msgBytes)));
				os.write(marker);
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}

	public void start() {
		inputStreams().forEach(in -> inputStreamDecoder(in, bytes -> {
			var msgBytes = Arrays.copyOf(bytes, bytes.length - 4);
			int hashCode = ByteBuffer.wrap(bytes, bytes.length - 4, 4).getInt();

			if (Arrays.hashCode(msgBytes) == hashCode) {
				processIncomingMessage((Message) serializer.fromBytes(bytes));
			} else {
				System.err.println("garbage");
			}
		}));
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
