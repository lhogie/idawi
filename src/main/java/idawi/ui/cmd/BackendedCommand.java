package idawi.ui.cmd;

import java.util.Set;

import idawi.Component;
import idawi.knowledge_base.ComponentRef;
import idawi.messaging.Message;
import idawi.messaging.ProgressMessage;
import idawi.routing.TargetComponents;
import j4u.CommandLine;
import toools.io.Cout;
import toools.io.file.RegularFile;
import toools.reflect.Clazz;

public abstract class BackendedCommand extends CommunicatingCommand {

	public BackendedCommand(RegularFile launcher) {
		super(launcher);
		addOption("--hook", "-k", ".+", null,
				"the CDL description of the component which will be the entry point to the overlay");
	}

	@Override
	protected int work(Component c, CommandLine cmdLine, double timeout) throws Throwable {
		ComponentRef hook = new ComponentRef(getOptionValue(cmdLine, "--hook"));
		Cout.info("connecting to overlay via " + hook);

		if (c.bb().ping(hook) == null) {
			Cout.error("Error pinging the hook");
			return 1;
		}

		Cout.info("executing command");
		Set<ComponentRef> target = Command.targetPeers(c, cmdLine.findParameters().get(0), msg -> Cout.warning(msg));

		CommandBackend backend = getBackend();
		backend.cmdline = cmdLine;
		var col = c.defaultRoutingProtocol().exec(CommandsService.exec.class, null,
				new TargetComponents.Multicast(target), true, backend).returnQ.collect(1, 1, c2 -> {
					var msg = c2.messages.last();
					if (msg.isError()) {
						((Throwable) msg.content).printStackTrace();
					} else if (msg.isProgress()) {
						System.out.println("progress; " + msg.content);
					} else {
						System.out.println(msg.content);
					}
				});

		if (col.stop) {
			System.err.println("not enough results!");
		}

		return 0;
	}

	private CommandBackend getBackend() {
		String backendClassName = getClass().getName() + "Backend";
		Class<CommandBackend> backendClass = Clazz.findClassOrFail(backendClassName);
		return Clazz.makeInstance(backendClass);
	}

	private void newReturn(Message feedback, Set<ComponentRef> peers) {

		if (feedback.content instanceof ProgressMessage) {
			progress(feedback.route.initialEmission().component, (ProgressMessage) feedback.content);
		} else if (feedback.content instanceof Throwable) {
			System.err.println("the following error occured on " + feedback.route.initialEmission());
			((Throwable) feedback.content).printStackTrace();
		} else {
			String text = feedback.content.toString();

			// if multiple peers may send feedback, we need to
			// distinguish their output
			if (peers == null || peers.size() > 1) {
				// if it's a multiline message
				if (text.contains("\n")) {
					System.out.println(feedback.route.initialEmission() + " says:");
				} else {
					System.out.print(feedback.route.initialEmission() + " says: \t");
				}
			}

			System.out.println(text);
		}

	}

	protected void progress(ComponentRef peer, ProgressMessage progress) {
		System.out.println("..." + peer + "\t..." + progress);
	}
}
