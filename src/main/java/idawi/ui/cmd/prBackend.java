package idawi.ui.cmd;

import java.util.List;
import java.util.function.Consumer;

import idawi.Component;
import idawi.ComponentInfo;
import idawi.PeerRegistry;
import toools.io.file.RegularFile;

public class prBackend extends CommandBackend {

	@Override
	public void runOnServer(Component n, Consumer<Object> out)
			throws Throwable {
		List<String> parms = cmdline.findParameters();
		String action = parms.remove(0);

		if (action.equals("list")) {
			n.descriptorRegistry.forEach(p -> out.accept(p.toString()));
		}
		else if (action.equals("add")) {
			while ( ! parms.isEmpty()) {
				n.descriptorRegistry.add(ComponentInfo.fromPDL(parms.remove(0)));
			}
		}
		else if (action.equals("save")) {
			RegularFile f = new RegularFile(Component.directory, "peers");
			f.getParent().mkdirs();
			f.setContentAsJavaObject(n.descriptorRegistry);
		}
		else if (action.equals("load")) {
			RegularFile f = new RegularFile(Component.directory, "peers");
			n.descriptorRegistry = (PeerRegistry) f.getContentAsJavaObject();
		}
	}
}
