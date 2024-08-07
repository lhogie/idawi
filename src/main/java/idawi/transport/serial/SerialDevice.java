package idawi.transport.serial;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fazecast.jSerialComm.SerialPort;

import idawi.messaging.Message;
import toools.util.Conversion;

public class SerialDevice {
	public SerialPort p;
	public List<MarkerManager> markers = new ArrayList<>();
	public static final byte[] msgMarker = "fgmfkdjgvhdfkghksfjhfdsj".getBytes();

	public SerialDevice(SerialPort p) {
		this.p = p;
		markers.add(new MarkerManager() {

			@Override
			public byte[] marker() {
				return msgMarker;
			}

			@Override
			public void callBack(byte[] bytes, SerialDriver serialDriver) {
				var msgBytes = Arrays.copyOf(bytes, bytes.length - 4);
				int hashCode = ByteBuffer.wrap(bytes, bytes.length - 4, 4).getInt();

				if (Arrays.hashCode(msgBytes) == hashCode) {
					Message testBytes = (Message) serialDriver.serializer.fromBytes(bytes);
					System.out.println(testBytes);
					serialDriver.processIncomingMessage((Message) serialDriver.serializer.fromBytes(bytes));
				} else {
					System.err.println("garbage");
				}
			}
		});
	}

	public void bcast(byte[] msgBytes) {
		OutputStream os = p.getOutputStream();
		try {
			var b = new ByteArrayOutputStream();
			b.write(msgBytes);
			b.write(Conversion.intToBytes(Arrays.hashCode(msgBytes)));
			b.write(msgMarker);
			System.out.println(b.toByteArray());
			System.out.println((Arrays.hashCode(msgBytes)));
			os.write(b.toByteArray());
			System.out.println("writing done");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
