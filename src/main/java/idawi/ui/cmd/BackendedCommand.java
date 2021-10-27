package idawi.ui.cmd;

import java.util.Set;

import idawi.ComponentDescriptor;
import idawi.Message;
import idawi.ProgressMessage;
import idawi.Service;
import idawi.ServiceAddress;
import idawi.service.PingService;
import j4u.CommandLine;
import toools.io.Cout;
import toools.io.file.RegularFile;
import toools.reflect.Clazz;

public abstract class BackendedCommand extends CommunicatingCommand {

	public BackendedCommand(RegularFile launcher) {
		super(launcher);
		addOption("--hook", "-k", ".+", null,
				"the CDL description of the component which wille be the entry point to the overlay");
	}

	@Override
	protected int work(Service localService, CommandLine cmdLine, double timeout) throws Throwable {
		ComponentDescriptor hook = ComponentDescriptor.fromCDL(getOptionValue(cmdLine, "--hook"));
		Cout.info("connecting to overlay via " + hook);

		if (PingService.ping(localService, hook, timeout) == null) {
			Cout.error("Error pinging the hook");
			return 1;
		}

		Cout.info("executing command");
		var to = new ServiceAddress(
				Command.targetPeers(localService.component, cmdLine.findParameters().get(0), msg -> Cout.warning(msg)),
				CommandsService.class);

		CommandBackend backend = getBackend();
		backend.cmdline = cmdLine;

		if (!localService.start(to, CommandsService.exec, true, backend).returnQ.forEach2(msg -> {
			if (msg.isError()) {
				((Throwable) msg.content).printStackTrace();
			} else if (msg.isProgress()) {
				System.out.println("progress; " + msg.content);
			}
		})) {
			System.err.println("Timeout!");
		}

		return 0;
	}

	private CommandBackend getBackend() {
		String backendClassName = getClass().getName() + "Backend";
		Class<CommandBackend> backendClass = Clazz.findClassOrFail(backendClassName);
		return Clazz.makeInstance(backendClass);
	}

	private void newReturn(Message feedback, Set<ComponentDescriptor> peers) {

		if (feedback.content instanceof ProgressMessage) {
			progress(feedback.route.source().component, (ProgressMessage) feedback.content);
		} else if (feedback.content instanceof Throwable) {
			System.err.println("the following error occured on " + feedback.route.source());
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

	protected void progress(ComponentDescriptor peer, ProgressMessage progress) {
		System.out.println("..." + peer + "\t..." + progress);
	}
}
