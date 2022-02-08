package idawi.net;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import idawi.Component;
import idawi.ComponentDescriptor;
import idawi.Message;
import toools.io.Cout;
import toools.io.file.RegularFile;
import toools.text.TextUtilities;
import toools.thread.Threads;

public class PipeFromToParentProcess extends TransportLayer {
	private boolean run = false;
	private final boolean suicideIfLooseParent;
//	private final ComponentDescriptor parent;
	private final Set<ComponentDescriptor> neighbors;

	public PipeFromToParentProcess(Component c, ComponentDescriptor parent, boolean suicideWhenParentDies) {
		super(c);
		this.suicideIfLooseParent = suicideWhenParentDies;
//		this.parent = parent;
		this.neighbors = Collections.singleton(parent);
	}

	@Override
	public Set<ComponentDescriptor> neighbors() {
		return neighbors;
	}

	@Override
	public void send(Message msg, Collection<ComponentDescriptor> neighbors) {
		if (!run)
			return;

		if (!neighbors.equals(neighbors))
			throw new IllegalStateException();

		try {
//			synchronized (Cout.raw_stdout) {
				Cout.raw_stdout.println(PipeFromToChildProcess.msgMark);
				serializer.write(msg, Cout.raw_stdout);
//			}
		} catch (IOException e) {
			//new RegularFile("$HOME/err.txt").setContentAsASCII(TextUtilities.exception2string(e));
			if (suicideIfLooseParent) {
				System.exit(0);
			}
		}
	}

	@Override
	public String getName() {
		return "sdtin/stdout";
	}

	@Override
	public boolean canContact(ComponentDescriptor c) {
		return neighbors.contains(c);
	}

	@Override
	public void injectLocalInfoTo(ComponentDescriptor c) {
	}

	static int nbR = 0;

	@Override
	public void start() {
		run = true;

		Threads.newThread_loop(() -> run, () -> {
			try {
//				System.out.println("reading stdin        " + nbR);
//				System.out.println(System.in.available() + " bytes available");
				Message msg = (Message) serializer.read(System.in);
//				System.out.println("reading ok " + nbR);
				nbR++;
				processIncomingMessage(msg);
			} catch (Exception e) {
				new RegularFile("$HOME/err.txt").setContentAsASCII(TextUtilities.exception2string(e));
				if (suicideIfLooseParent) {
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
