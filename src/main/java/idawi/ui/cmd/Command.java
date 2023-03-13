package idawi.ui.cmd;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import idawi.Component;
import idawi.knowledge_base.ComponentRef;
import idawi.knowledge_base.MapService;
import idawi.knowledge_base.MiscKnowledgeBase;
import j4u.CommandLineApplication;
import j4u.License;
import toools.io.Cout;
import toools.io.file.RegularFile;
import toools.reflect.Clazz;

public abstract class Command extends CommandLineApplication {

	static {
		Cout.timestamp();
	}

	public Command(RegularFile launcher) {
		super(launcher);
	}

	public String getCommandName() {
		return Clazz.classNameWithoutPackage(getClass().getName());
	}

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

	public static Set<ComponentRef> targetPeers(Component n, String list, Consumer<Object> out) {
		Set<ComponentRef> peers = new HashSet<>();

		for (String p : list.split(" *, *")) {
			if (p.equals("_")) {
				peers.add(n.ref());
			} else {
				var pp = n.lookup(MapService.class).map.lookup(p);

				if (pp == null) {
					out.accept("no component with name: " + p);
				} else {
					peers.add(pp);
				}
			}
		}

		if (peers.isEmpty()) {
			out.accept("no such peer");
		}

		return peers;
	}
}
