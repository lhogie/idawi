package idawi.transport;

import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import idawi.Component;
import idawi.messaging.Message;

public class PipeFromToParentProcess extends TransportService {
	private final boolean suicideIfLoseParent;
	private final Set<Component> parent;

	public PipeFromToParentProcess(Component me, Component parent, boolean suicideWhenParentDies) {
		super(me);
		this.suicideIfLoseParent = suicideWhenParentDies;
		this.parent = Collections.singleton(parent);

		new Thread(() -> {
			try {
				processIncomingMessage((Message) serializer.read(System.in));
			} catch (Exception e) {
				e.printStackTrace();

				if (suicideIfLoseParent) {
					System.exit(0);
				}
			}
		}).start();
	}

	@Override
	public Set<Component> actualNeighbors() {
		return parent;
	}

	@Override
	protected void multicastImpl(Message msg, Collection<OutNeighbor> neighbors) {
		if (!neighbors.equals(actualNeighbors()))
			throw new IllegalStateException();

		bcastImpl(msg);
	}

	@Override
	protected void bcastImpl(Message msg) {
		sysout(msg);
	}

	public static void sysout(Object o) {
		var bytes = serializer.toBytes(o);
		var base64 = new String(Base64.getEncoder().encode(bytes));
		base64 = base64.replace("\n", "");
		System.out.println(PipeFromToChildProcess.base64ObjectMark + base64);
	}

	@Override
	public String getName() {
		return "pipe to parent";
	}

	@Override
	public boolean canContact(Component c) {
		return parent.contains(c);
	}

}
