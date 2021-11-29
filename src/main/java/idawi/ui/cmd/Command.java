package idawi.ui.cmd;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import idawi.Component;
import idawi.ComponentDescriptor;
import idawi.RegistryService;
import j4u.CommandLineApplication;
import j4u.License;
import toools.io.Cout;
import toools.io.file.RegularFile;
import toools.reflect.Clazz;

public abstract class Command extends CommandLineApplication {

	static {
		Cout.activate();
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

	public static Set<ComponentDescriptor> targetPeers(Component n, String list, Consumer<Object> out) {
		Set<ComponentDescriptor> peers = new HashSet<>();

		for (String p : list.split(" *, *")) {
			if (p.equals("_")) {
				peers.add(n.descriptor());
			} else {
				var pp = n.lookupOperation(RegistryService.lookUp.class).lookup(p);

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
