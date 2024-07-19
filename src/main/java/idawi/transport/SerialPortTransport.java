package idawi.transport;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import com.fazecast.jSerialComm.SerialPort;

import idawi.Component;

public class SerialPortTransport extends InputStreamBasedDriver {

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
	protected void multicast(byte[] msg, Collection<Link> outLinks) {
		SerialPo
//		Cout.debug(outLinks);
//		outLinks.forEach(l -> l.dest.processIncomingMessage((Message) l.dest.serializer.fromBytes(msg)));
		fakeSend(msg, outLinks);
	}

	@Override
	protected void bcast(byte[] msg) {
		multicast(msg, activeOutLinks());
	}

	@Override
	protected Stream<InputStream> inputStream() {
		return Arrays.stream(SerialPort.getCommPorts()).map(p -> p.getInputStream());
	}
}
