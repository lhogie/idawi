package idawi.transport;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;

import idawi.messaging.Message;

public abstract class InputStreamBasedDriver extends TransportService {

	public static final byte[] marker = "fgmfkdjgvhdfkghksfjhfdsj".getBytes();

	@Override
	protected void multicast(byte[] msgBytes, Collection<Link> outLinks) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void bcast(byte[] msgBytes) {
		// TODO Auto-generated method stub

	}

	protected abstract Stream<InputStream> inputStreams();

	public void start() {
		inputStreams().forEach(in -> {
			var buf = new byte[marker.length];
			int sz = 0;

			if (sz == marker.length) {
				boolean markerFound = Arrays.equals(buf, 0, marker.length, marker, 0, marker.length);

				if (markerFound) {
					in.read(buf, 0, 4);
					int len = ByteBuffer.wrap(buf, 0, 4).getInt();
					var msgData = in.readNBytes(len);
					in.read(buf, 0, 4);
					int hashCode = ByteBuffer.wrap(buf, 0, 4).getInt();

					if (Arrays.hashCode(buf) == hashCode) {
						var msg = (Message) serializer.fromBytes(buf);
						processIncomingMessage(msg);
					}
				}
			}
		});
	}
}
