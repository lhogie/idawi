package idawi.demo.valentin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import idawi.Component;
import idawi.InnerClassEndpoint;
import idawi.Service;
import idawi.TypedInnerClassEndpoint;
import idawi.messaging.MessageQueue;
import idawi.routing.ComponentMatcher;
import idawi.routing.RoutingService;
import toools.io.file.Directory;
import toools.io.file.RegularFile;

public class ChordService extends Service {
	// item data will be stored there
	public static Directory pathToLocalContents = new Directory("$HOME/i3s/idawi/chord");
	final public Directory directory;

	// chord queries will be send using this routing scheme
	public RoutingService rp = component.defaultRoutingProtocol();

	public ChordService(Component c) {
		super(c);
		directory = new Directory(pathToLocalContents, c.friendlyName);
		directory.mkdirs();
	}

	public void store(Item item) {
		System.out.println("add item via " + component);

		// computes the set of component that will host the item
		Set<Component> h = hash(item.key);
		System.out.println("to " + h);

		// async multicast the item to all targets
		var target = ComponentMatcher.multicast(h);
		rp.exec(ChordService.class, set.class, rp.defaultData(), target, null, item, true);
	}

	public Set<String> localKeys() {
		return directory.listRegularFiles().stream().map(f -> file2ItemKey(f)).collect(Collectors.toSet());
	}

	private static String file2ItemKey(RegularFile f) {
		return f.getName().replaceFirst(".dat$", "");
	}

	public Item get(String key) {
		var hosts = hash(key);

		// try all components in a sequence
		for (var c : hosts) {
			// sync call
			var msg = rp.exec(ChordService.class, get.class, rp.defaultData(), ComponentMatcher.unicast(c), true, key,
					true).returnQ.poll_sync();

			if (msg.content instanceof Item) {
				return (Item) msg.content;
			}
		}

		return null;
	}

	public static class ItemLocation {
		Item item;
		Component from;

		ItemLocation(Item i, Component c) {
			this.item = i;
			this.from = c;
		}
	}

	public List<ItemLocation> search(String k, double searchDuration) {
		// SYNC multicast the key to all targets
		return rp.exec(ChordService.class, get.class, k, true).returnQ.collector()
				.collectDuring(searchDuration).messages.stream().filter(msg -> msg.content instanceof Item)
				.map(msg -> new ItemLocation((Item) msg.content, msg.route.source())).toList();
	}

	public Set<Component> hash(String key) {
		var components = new ArrayList<>(
				component.localView().g.findLinks(l -> l.isActive()).stream().map(l -> l.dest.component).toList());
		components.remove(component);

		int nbTargets = 2;

		if (components.size() < nbTargets)
			return null;

		var r = new HashSet<Component>();

		for (int i = 0; i < nbTargets; ++i) {
			// compute a hash for that target
			r.add(components.get((key.hashCode() + "/" + i).hashCode() % components.size()));
		}

		return r;
	}

	public class keys extends TypedInnerClassEndpoint {
		public Collection<String> keys() {
			return localKeys();
		}

		@Override
		public String getDescription() {
			return "list the name of the entries stored in this component";
		}
	}

	public class set extends TypedInnerClassEndpoint {
		public void f(Item i) {
			System.out.println(component + " writing " + file(i.key));
			file(i.key).setContent(i.content);
		}

		@Override
		public String getDescription() {
			return "stores this items locally";
		}
	}

	private RegularFile file(String key) {
		return new RegularFile(directory, key + ".dat");
	}

	public class get extends InnerClassEndpoint {

		@Override
		public void impl(MessageQueue in) throws Throwable {
			// get the exec message
			var execMsg = in.poll_sync();

			// extract the key param from it
			String k = (String) execMsg.content;

			// get the corresponding items
			for (var f : directory.listRegularFiles()) {
				if (file2ItemKey(f).equals(k)) {
					var i = new Item(k, file(k).getContent());
					reply(execMsg, i, true);
					return;
				}
			}
		}

		@Override
		public String getDescription() {
			return "retrieves all items matching the given name";
		}
	}

	public class deleteRegex extends TypedInnerClassEndpoint {
		public void delete(String k) {
			directory.listRegularFiles().stream().filter(f -> file2ItemKey(f).equals(k)).findAny()
					.ifPresent(f -> f.delete());
		}

		@Override
		public String getDescription() {
			return "stores this items locally";
		}
	}
}
