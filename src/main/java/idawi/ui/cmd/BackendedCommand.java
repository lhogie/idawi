package idawi.ui.cmd;

import java.util.Set;

import idawi.Component;
import idawi.messaging.Message;
import idawi.messaging.ProgressMessage;
import idawi.routing.ComponentMatcher;
import idawi.ui.cmd.CommandsService.exec;
import j4u.CommandLine;
import j4u.CommandLineSpecification;
import toools.io.Cout;
import toools.reflect.Clazz;

public abstract class BackendedCommand extends CommunicatingCommand {

	@Override
	protected void specifyCmdLine(CommandLineSpecification spec) {
		spec.addOption("--hook", "-k", ".+", null,
				"the friendly name of the component which will be the entry point to the overlay");
	}

	@Override
	protected int work(Component localNode, CommandLine cmdLine, double timeout) throws Throwable {
		var hook = new Component();
		hook.friendlyName = getOptionValue(cmdLine, "--hook");
		Cout.info("connecting to overlay via " + hook);

		if (localNode.bb().ping(hook) == null) {
			Cout.error("Error pinging the hook");
			return 1;
		}

		Cout.info("executing command");
		var target = IdawiCommand.targetPeers(localNode, cmdLine.findParameters().get(0), msg -> Cout.warning(msg));

		CommandBackend backend = getBackend();
		backend.cmdline = cmdLine;
		var col = localNode.defaultRoutingProtocol().exec(CommandsService.class, exec.class, null,
				ComponentMatcher.multicast(target), true, backend, true).returnQ.collector();

		col.collect(1, 1, c2 -> {
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

	private void newReturn(Message feedback, Set<Component> peers) {

		if (feedback.content instanceof ProgressMessage) {
			progress(feedback.route.source(), (ProgressMessage) feedback.content);
		} else if (feedback.content instanceof Throwable) {
			// System.err.println("the following error occured on " +
			// feedback.route.initialEmission());
			((Throwable) feedback.content).printStackTrace();
		} else {
			String text = feedback.content.toString();

			// if multiple peers may send feedback, we need to
			// distinguish their output
			if (peers == null || peers.size() > 1) {
				// if it's a multiline message
				if (text.contains("\n")) {
					System.out.println(feedback.route.source() + " says:");
				} else {
					System.out.print(feedback.route.source() + " says: \t");
				}
			}

			System.out.println(text);
		}

	}

	protected void progress(Component peer, ProgressMessage progress) {
		System.out.println("..." + peer + "\t..." + progress);
	}
}
