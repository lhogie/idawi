package idawi.net;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import idawi.ComponentInfo;
import idawi.Message;
import idawi.TransportLayer;
import toools.io.Cout;
import toools.thread.Threads;

public class PipeFromToParentProcess extends TransportLayer {
	private boolean run = false;
	private final boolean suicideWhenParentDies;
	private final ComponentInfo parent;

	public PipeFromToParentProcess(ComponentInfo parent, boolean suicideWhenParentDies) {
		this.suicideWhenParentDies = suicideWhenParentDies;
		this.parent = parent;
	}

	@Override
	public Set<ComponentInfo> neighbors() {
		return Collections.singleton(parent);
	}

	@Override
	public void send(Message msg, Collection<ComponentInfo> neighbors) {
		if ( ! run)
			return;

		if (neighbors.size() != 1 || ! neighbors.iterator().next().equals(parent))
			throw new IllegalStateException();

		try {
			synchronized (Cout.raw_stdout) {
				Cout.raw_stdout.println(PipeFromToChildProcess.msgMark);
				serializer.write(msg, Cout.raw_stdout);
			}
		}
		catch (IOException e) {
			if (suicideWhenParentDies) {
				System.exit(0);
			}
		}
	}

	@Override
	public String getName() {
		return "SSParent";
	}

	@Override
	public boolean canContact(ComponentInfo c) {
		return c.equals(parent);
	}

	@Override
	public void injectLocalInfoTo(ComponentInfo c) {
	}

	@Override
	public void start() {
		run = true;

		Threads.newThread_loop(() -> run, () -> {
			try {
				Message msg = (Message) serializer.read(System.in);
				processIncomingMessage(msg);
			}
			catch (IOException e) {
				if (suicideWhenParentDies) {
					System.exit(0);
				}
			}
		});
	}

	@Override
	protected void stop() {
		run = false;
	}
}
