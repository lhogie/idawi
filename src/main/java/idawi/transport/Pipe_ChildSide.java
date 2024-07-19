package idawi.transport;

import java.io.IOException;
import java.util.Base64;
import java.util.Collection;

import idawi.Component;
import idawi.Idawi;
import idawi.messaging.Message;
import toools.io.Cout;

public class Pipe_ChildSide extends TransportService implements Broadcastable {
	public boolean suicideIfLoseParent = true;

	public Pipe_ChildSide(Component me) {
		super(me);

		Idawi.agenda.threadPool.submit(() -> {
			try {
				while (true) {
					var msg = (Message) serializer.read(System.in);
					processIncomingMessage(msg);
				}
			} catch (Exception e) {
				e.printStackTrace();

				if (suicideIfLoseParent) {
					System.exit(0);
				}
			}
		});
	}

	@Override
	protected void multicast(byte[] msgBytes, Collection<Link> outLinks) {
		send(msgBytes);
	}

	@Override
	public void bcast(byte[] msgBytes) {
		send(msgBytes);
	}

	public void send(byte[] o) {
		sendBytes(o);
	}

	public static void sendBytes(byte[] bytes) {
		var base64 = new String(Base64.getEncoder().encode(bytes));
		base64 = base64.replace("\n", "");
		System.out.println(Pipe_ParentSide.base64ObjectMark + base64);
	}

	@Override
	public String getName() {
		return getClass().getSimpleName();
	}

	@Override
	public void dispose(Link l) {
		try {
			System.in.close();
		} catch (IOException e) {
		}

		System.out.close();
	}

	@Override
	public double latency() {
		return 0;
	}

}
