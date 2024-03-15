package idawi.ui.cmd;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import idawi.Component;
import idawi.service.local_view.LocalViewService;
import toools.io.Cout;
import toools.reflect.Clazz;

public abstract class IdawiCommand extends j4u.Command {

	static {
		Cout.timestamp();
	}

	public String getCommandName() {
		return Clazz.classNameWithoutPackage(getClass().getName());
	}


	public static Set<Component> targetPeers(Component n, String list, Consumer<Object> out) {
		Set<Component> peers = new HashSet<>();

		for (String p : list.split(" *, *")) {
			if (p.equals("_")) {
				peers.add(n);
			} else {
				var pp = n.service(LocalViewService.class).g.findComponentByFriendlyName(p);

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
