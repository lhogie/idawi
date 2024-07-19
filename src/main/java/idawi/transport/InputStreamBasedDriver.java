package idawi.transport;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;

import idawi.Component;
import idawi.messaging.Message;

public abstract class InputStreamBasedDriver extends TransportService {

	public InputStreamBasedDriver(Component c) {
		super(c);
		// TODO Auto-generated constructor stub
	}

	public static final byte[] marker = "fgmfkdjgvhdfkghksfjhfdsj".getBytes();



	protected abstract Stream<InputStream> inputStreams();

	public void start() {
		inputStreams().forEach(in -> {
			try {
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
			}
			catch (IOException err) {
				System.err.println("I/O error reading stream");
			}
		});
	}
}
