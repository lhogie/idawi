package idawi.ui;

import java.net.InetAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import idawi.Component;
import idawi.ComponentInfo;
import j4u.CommandLine;
import toools.io.file.RegularFile;

public class AgentMain extends JThingLineCmd {

	public AgentMain(RegularFile launcher) {
		super(launcher);
		addOption("--link", null, null, null, "suicide when launcher dies");
	}

	public static void main(String[] args) throws Throwable {
		new AgentMain(null).run(args);
	}

	@Override
	public int runScript(CommandLine cmdLine) throws Throwable {
		boolean link = isOptionSpecified(cmdLine, "--link");
		List<String> parms = cmdLine.findParameters();

		if (parms.isEmpty()) {
			parms.add(System.getProperty("user.name") + "@"
					+ InetAddress.getLocalHost().getHostName());
		}

		Set<Component> peers = new HashSet<>();

		for (int i = 0; i < parms.size(); ++i) {
			String name = parms.get(i);
			Component peer = new Component(ComponentInfo.fromPDL("name=" + name));
			ComponentInfo c = new ComponentInfo();
			c.friendlyName = name;
			peers.add(peer);
		}

		for (Component p : peers) {
			for (Component p2 : peers) {
				if (p != p2) {
					p.descriptorRegistry.add(p2.descriptor());
				}
			}
		}

		return 0;
	}

	@Override
	public String getShortDescription() {
		return "start nodes";
	}
}
