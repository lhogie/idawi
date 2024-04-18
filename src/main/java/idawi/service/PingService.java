package idawi.service;

import idawi.Component;
import idawi.InnerClassEndpoint;
import idawi.Service;
import idawi.messaging.MessageQueue;
import toools.io.Cout;

public class PingService extends Service {
	public PingService(Component component) {
		super(component);
	}

	public class ping extends InnerClassEndpoint<Object, Void> {

		@Override
		public void impl(MessageQueue in) {
			var m = in.poll_sync();
			Cout.debugSuperVisible("PIINNGGG");
			// sends back the ping message to the caller
			sendd(m, m.replyTo, mmsg -> mmsg.eot = true);
		}

		@Override
		public String getDescription() {
			return "sends the message back";
		}
	}
}
