package idawi.ui.cmd;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import idawi.Component;
import idawi.ComponentInfo;
import idawi.Service;
import j4u.CommandLine;
import j4u.License;
import toools.io.Cout;
import toools.io.file.RegularFile;
import toools.reflect.Clazz;

public abstract class CommunicatingCommand extends Command {

	static {
		Cout.activate();
	}

	public CommunicatingCommand(RegularFile launcher) {
		super(launcher);
		addOption("--timeout", "-t", ".*", 1, "timeout in second");
		addOption("--repeat", "-r", "[0-9]+", "1", "repeats the command the given number of times");

	}

	public String getCommandName() {
		return Clazz.classNameWithoutPackage(getClass().getName());
	}

	@Override
	public int runScript(CommandLine cmdLine) throws Throwable {
		double timeout = Double.valueOf(getOptionValue(cmdLine, "--timeout"));

		Component localNode = new Component(ComponentInfo.fromPDL("name="+getCommandName()));
		Service localService = new Service(localNode) {
			@Override
			public String getFriendlyName() {
				return "local service";
			}
		};

		int repeat = Integer.valueOf(cmdLine.getOptionValue("--repeat"));

		for (int i = 0; i < repeat; ++i) {

			if (repeat > 1) {
				Cout.debugSuperVisible("run #" + (i + 1));
			}

			int exitCode = work(localService, cmdLine, timeout);

			if (exitCode != 0) {
				return exitCode;
			}
		}

		if (repeat > 1) {
			Cout.debugSuperVisible("completed");
		}

		return 0;
	}

	protected abstract int work(Service localService, CommandLine cmdLine, double timeout) throws Throwable;

	@Override
	public String getAuthor() {
		return "Luc Hogie";
	}

	@Override
	public License getLicence() {
		return License.ApacheLicenseV2;
	}

	@Override
	public String getApplicationName() {
		return "jThing";
	}

	@Override
	public String getYear() {
		return "2019-2020";
	}

	public static Set<ComponentInfo> targetPeers(Component n, String list, Consumer<Object> out) {
		Set<ComponentInfo> peers = new HashSet<>();

		for (String p : list.split(" *, *")) {
			if (p.equals("_")) {
				peers.add(n.descriptor());
			} else {
				var pp = n.descriptorRegistry.lookupByName(p);

				if (pp.isEmpty()) {
					out.accept("no peer with name: " + p);
				} else if (pp.size() > 1) {
					out.accept(pp.size() + " peers with name: " + p);
				}

				peers.addAll(pp);
			}
		}

		if (peers.isEmpty()) {
			out.accept("no such peer");
		}

		return peers;
	}
}
