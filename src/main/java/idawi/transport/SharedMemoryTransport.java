package idawi.transport;

import idawi.Component;
import idawi.Idawi;
import idawi.Agenda;
import idawi.messaging.Message;

public class SharedMemoryTransport extends TransportService {

	public SharedMemoryTransport(Component c) {
		super(c);
	}

	@Override
	public String getName() {
		return "shared mem";
	}

	@Override
	public boolean canContact(Component c) {
		return c != null;
	}

	@Override
	protected void sendImpl(Message msg) {
		System.out.println("send " + msg);

		var c = msg.clone(component.serializer);

		Idawi.agenda.threadPool.submit(() -> {
			try {
				c.route.last().link.dest.processIncomingMessage(c);
			} catch (Throwable e) {
				e.printStackTrace();
			}
		});
	}

	@Override
	public void dispose(Link l) {
//		l.activity.close();
	}

}
