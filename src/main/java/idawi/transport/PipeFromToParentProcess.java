package idawi.transport;

import java.io.IOException;
import java.util.Base64;

import idawi.Component;
import idawi.RuntimeEngine;
import idawi.messaging.Message;

public class PipeFromToParentProcess extends TransportService {
	public boolean suicideIfLoseParent = true;
	private final Component parent;

	public PipeFromToParentProcess(Component me, Component parent) {
		super(me);
		this.parent = parent;

		RuntimeEngine.threadPool.submit(() -> {
			try {
				while (true) {
					processIncomingMessage((Message) component.serializer.read(System.in));
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
	protected void sendImpl(Message msg) {
		send(msg);
	}

	public void send(Object o) {
		sendBytes(component.serializer.toBytes(o));
	}

	public static void sendBytes(byte[] bytes) {
		var base64 = new String(Base64.getEncoder().encode(bytes));
		base64 = base64.replace("\n", "");
		System.out.println(PipesFromToChildrenProcess.base64ObjectMark + base64);
	}

	@Override
	public String getName() {
		return "pipe to parent";
	}

	@Override
	public boolean canContact(Component c) {
		return parent.equals(c);
	}

	@Override
	public void dispose(Link l) {
		if (!l.dest.component.equals(parent))
			throw new IllegalStateException();

		try {
			System.in.close();
		} catch (IOException e) {
		}

		System.out.close();
	}
}
