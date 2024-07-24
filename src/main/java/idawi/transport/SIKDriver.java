package idawi.transport;

import idawi.Component;
import idawi.Idawi;
import idawi.messaging.Message;
import idawi.routing.ComponentMatcher.all;
import idawi.service.serialTest.serialTest;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

public class SIKDriver extends JSerialCommTransport implements Broadcastable {

	public SIKDriver(Component c) {
		super(c);

		// INIT
	}

	public void setPower(double watt) {
		// TODO
	}

	public void setChannel(double watt) {
		// TODO
	}

}
