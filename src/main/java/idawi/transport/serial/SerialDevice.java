package idawi.transport.serial;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fazecast.jSerialComm.SerialPort;

import idawi.Idawi;
import idawi.messaging.Message;
import toools.thread.Q;
import toools.util.Conversion;

public class SerialDevice {
	protected SerialPort serialPort;
	public List<Callback> markers = new ArrayList<>();
	public static final byte[] msgMarker = "fgmfkdjgvhdfkghksfjhfdsj".getBytes();
	public Q<Object> rebootQ = new Q<>(1);
	public boolean rebooting;
	Q<Config> configQ = new Q<>(1);

	public SerialDevice(SerialPort p) {
		this.serialPort = p;
		markers.add(new Callback() {

			@Override
			public byte[] marker() {
				return msgMarker;
			}

			@Override
			public void callback(byte[] bytes, SerialDriver serialDriver) {
				var msgBytes = Arrays.copyOf(bytes, bytes.length - 4);
				int hashCode = ByteBuffer.wrap(bytes, bytes.length - 4, 4).getInt();

				if (Arrays.hashCode(msgBytes) == hashCode) {
					Message testBytes = (Message) serialDriver.serializer.fromBytes(bytes);
					System.out.println("Message Arrived :" + testBytes);
					serialDriver.processIncomingMessage((Message) serialDriver.serializer.fromBytes(bytes));
				} else {
					System.err.println("garbage");
				}
			}
		});
	}

	void newThread(SerialDriver driver) {
		Idawi.agenda.threadPool.submit(() -> {
			var buf = new MyByteArrayOutputStream();
			var inputStream = serialPort.getInputStream();
			try {

				while (true) {
					if ((inputStream.available() == 0) && buf.endsByData()) {
						dataParse(buf.toByteArray(), driver);
					}
					int i = inputStream.read();
					if (i == -1) {
						return;
					}

					buf.write((byte) i);
					for (var callback : markers) {
						if (buf.endsBy(callback.marker())) {
							System.out.println("Before callback");

							callback.callback(buf.toByteArray(), driver);
							System.out.println("After callback");
							buf.reset();
						}
					}
				}

			} catch (IOException err) {
				System.err.println("I/O error reading stream");
			}
		});
	}

	public void bcast(byte[] msgBytes) {
		OutputStream os = serialPort.getOutputStream();
		try {
			var b = new ByteArrayOutputStream();
			b.write(msgBytes);
			b.write(Conversion.intToBytes(Arrays.hashCode(msgBytes)));
			b.write(msgMarker);
			System.out.println(b.toByteArray());
			System.out.println((Arrays.hashCode(msgBytes)));
			os.write(b.toByteArray());
			// System.out.println("writing done");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String getName() {
		return serialPort.getDescriptivePortName();
	}

	public void dataParse(byte[] bytes, SerialDriver serialDriver) {

		System.out.println("callback AT :");
		var config = Config.from(new String(bytes));
		System.out.println("Callback parsed and data added to configQ");
		configQ.add_sync(config);

	}

}
