package idawi.transport;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;

import com.fazecast.jSerialComm.SerialPort;

import idawi.Component;
import idawi.Idawi;
import idawi.messaging.Message;
import idawi.routing.ComponentMatcher.all;
import idawi.service.serialTest.serialTest;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

public class SIKDriver extends InputStreamBasedDriver implements Broadcastable {
	public static final byte[] marker = "fgmfkdjgvhdfkghksfjhfdsj".getBytes();
	public static final long serialVersionUID = 6207309244483020844L;
	public static SerialPort comPort = SerialPort.getCommPort("COM8");

	public SIKDriver(Component c) {
		super(c);
		comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 0, 0);
		comPort.setBaudRate(57600);
		comPort.setFlowControl(SerialPort.FLOW_CONTROL_RTS_ENABLED | SerialPort.FLOW_CONTROL_CTS_ENABLED);
		comPort.openPort();
	}

	public byte[] startSend(String data) {
		Message msg = new Message<>();
		msg.content = data;
		byte[] msgBytes = serializer.toBytes(msg);

		// byte[] concatBytes = ArrayUtils.addAll(marker, msgBytes);
		// System.out.println(marker);
		// System.out.println(msgBytes);
		// System.out.println(concatBytes);

		// return concatBytes;
		return msgBytes;
	}

	@Override
	public String getName() {
		return "serial send";
	}

	@Override
	public void dispose(Link l) {
		// l.activity.close();
	}

	@Override
	public double latency() {
		return 0;
	}

	@Override
	protected void multicast(byte[] msg, Collection<Link> outLinks) {

	}

	public void test(InputStream it, Consumer<byte[]> f) throws IOException {
		byte[] bufferReaderMarker = new byte[24];
		String allByte = new String();
		int byteCounter = 0;
		while (true) {
			char oneByte = (char) it.read();
			if (oneByte == -1) {
				break;
			}
			allByte = allByte + oneByte;
			if (byteCounter == 24) {
			}
			byteCounter++;
		}
	}

	@Override
	public void bcast(byte[] msg) {
		// String dataToSend = "Helcxvcxvcxlo";
		// String dataToSend2 = "Yafdvfdvfdkmp";

		// Component c = new Component();
		// serialTest serialObject = new serialTest(c);
		byte[] msg1 = msg;
		// byte[] msg2 = serialObject.startSend(dataToSend2);

		// SerialPort comPort = SerialPort.getCommPort("/dev/ttyUSB0");
		// SerialPort comPort = SerialPort.getCommPort("COM8");

		// comPort.setBaudRate(57600);
		// comPort.setFlowControl(SerialPort.FLOW_CONTROL_RTS_ENABLED |
		// SerialPort.FLOW_CONTROL_CTS_ENABLED);
		// comPort.openPort();

		// comPort2.setBaudRate(57600);
		// comPort2.setFlowControl(SerialPort.FLOW_CONTROL_RTS_ENABLED |
		// SerialPort.FLOW_CONTROL_CTS_ENABLED);
		// comPort2.openPort();
		try {
			byte[] lengthmsg = ByteBuffer.allocate(4).putInt(msg1.length).array();
			byte[] testlength = new byte[4];
			int intHashCode = Arrays.hashCode(msg1);
			byte[] hashCode = ByteBuffer.allocate(4).putInt(intHashCode).array();
			System.out.println(intHashCode);

			// byte[] lengthmsg2 = ByteBuffer.allocate(4).putInt(msg2.length).array();
			// System.out.println(ByteBuffer.wrap(lengthmsg2).getInt() + " " +
			// lengthmsg2.length);
			// int intHashCode2 = Arrays.hashCode(msg2);
			// byte[] hashCode2 = ByteBuffer.allocate(4).putInt(intHashCode2).array();
			// System.out.println(intHashCode2);
			// int bytesWrittenMarker = comPort.writeBytes(marker, marker.length);
			// Thread.sleep(500);
			// int bytesWrittenLength = comPort.writeBytes(lengthmsg, lengthmsg.length);
			// Thread.sleep(500);
			// int bytesWritten = comPort.writeBytes(msg1, msg1.length);
			byte[] allByteArray = new byte[msg1.length + hashCode.length + marker.length + msg1.length + hashCode.length
					+ marker.length + msg1.length + hashCode.length + marker.length];
			ByteBuffer buff = ByteBuffer.wrap(allByteArray);
			buff.put(msg1);
			buff.put(hashCode);
			buff.put(marker);
			System.out.println("all : " + allByteArray.length);
			byte[] combined = buff.array();
			int bytesWrittenCombined = comPort.writeBytes(combined, combined.length);

			ByteBuffer.wrap(combined).get(testlength, 0, 4);
			System.out
					.println(
							ByteBuffer.wrap(testlength).getInt() + " length of  message object :  " + lengthmsg.length);
			String t = new String(combined);

			System.out.println(combined + " written : " + bytesWrittenCombined);

			// byte[] allByteArray2 = new byte[marker.length + lengthmsg2.length +
			// msg2.length + hashCode2.length];
			// ByteBuffer buff2 = ByteBuffer.wrap(allByteArray2);
			// buff2.put(marker);
			// buff2.put(lengthmsg2);
			// buff2.put(msg2);
			// buff2.put(hashCode2);
			// byte[] combined2 = buff2.array();
			// int bytesWrittenCombined2 = comPort2.writeBytes(combined2, combined2.length);
			// // Thread.sleep(500);
			// System.out.println(combined2 + " " + bytesWrittenCombined2);

			// System.out.println(marker + " " + bytesWrittenMarker);
			// System.out.println(lengthmsg + " " + bytesWrittenLength);
			// System.out.println(msg1 + " " + bytesWritten);

		} catch (Exception e) {
			e.printStackTrace();
		}
		// comPort2.closePort();

	}

	@Override
	protected Stream<InputStream> inputStreams() {
		return Arrays.stream(SerialPort.getCommPorts()).map(p -> p.getInputStream());
	}
}
