package idawi.transport;

import java.io.IOException;
import java.util.Base64;
import java.util.Collection;
import java.util.Set;

import idawi.Component;
import idawi.RuntimeEngine;
import idawi.messaging.Message;

public class PipeFromToParentProcess extends TransportService {
	private final boolean suicideIfLoseParent;
	private final Component parent;

	public PipeFromToParentProcess(Component me, Component parent, boolean suicideWhenParentDies) {
		super(me);
		this.suicideIfLoseParent = suicideWhenParentDies;
		this.parent = parent;

		RuntimeEngine.threadPool.submit(() -> {
			try {
				while (true) {
					processIncomingMessage((Message) serializer.read(System.in));
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
		sysout(msg);
	}

	public static void sysout(Object o) {
		var bytes = serializer.toBytes(o);
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
