package idawi.ui.cmd;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import idawi.Component;
import idawi.ComponentDescriptor;
import idawi.RegistryService;
import toools.io.file.RegularFile;

public class prBackend extends CommandBackend {

	@Override
	public void runOnServer(Component n, Consumer<Object> out) throws Throwable {
		List<String> parms = cmdline.findParameters();
		String action = parms.remove(0);

		if (action.equals("list")) {
			n.lookupOperation(RegistryService.list.class).list().forEach(p -> out.accept(p.toString()));
		} else if (action.equals("add")) {
			while (!parms.isEmpty()) {
				n.lookupOperation(RegistryService.add.class).f(ComponentDescriptor.fromCDL(parms.remove(0)));
			}
		} else if (action.equals("save")) {
			RegularFile f = new RegularFile(Component.directory, "peers");
			f.getParent().mkdirs();
			f.setContentAsJavaObject(n.lookupOperation(RegistryService.list.class).list());
		} else if (action.equals("load")) {
			RegularFile f = new RegularFile(Component.directory, "peers");
			n.lookupOperation(RegistryService.addAll.class)
					.addAll((Set<ComponentDescriptor>) f.getContentAsJavaObject());
		}
	}
}
