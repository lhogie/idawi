package idawi.transport;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;

import com.fazecast.jSerialComm.SerialPort;

import idawi.Component;

public class SIKDriver extends InputStreamBasedDriver implements Broadcastable {

	public SIKDriver(Component c) {
		super(c);
	}

	@Override
	public String getName() {
		return "serial ports";
	}

	@Override
	public void dispose(Link l) {
//		l.activity.close();
	}

	@Override
	public double latency() {
		return 0;
	}

	@Override
	protected void multicast(byte[] msg, Collection<Link> outLinks) {
		new Exception().printStackTrace();

	}

	@Override
	public void bcast(byte[] msg) {
		new Exception().printStackTrace();

	}

	@Override
	protected Stream<InputStream> inputStreams() {
		return Arrays.stream(SerialPort.getCommPorts()).map(p -> p.getInputStream());
	}
}
