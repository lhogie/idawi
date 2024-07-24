package idawi.transport;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.stream.Stream;

import com.fazecast.jSerialComm.SerialPort;

import idawi.Component;

public class JSerialCommTransport extends StreamBasedDriver implements Broadcastable {

	public JSerialCommTransport(Component c) {
		super(c);

	}

	@Override
	public String getName() {
		return "serial ports";
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
	protected Stream<InputStream> inputStreams() {
		return Arrays.stream(SerialPort.getCommPorts()).map(p -> p.getInputStream());
	}

	@Override
	protected Stream<OutputStream> outputStreams() {
		return Arrays.stream(SerialPort.getCommPorts()).map(p -> p.getOutputStream());
	}

}
