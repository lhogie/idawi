package idawi.demo.valentin;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import idawi.Component;
import idawi.InnerClassEndpoint;
import idawi.Service;
import idawi.TypedInnerClassEndpoint;
import idawi.messaging.MessageQueue;
import idawi.routing.ComponentMatcher;
import idawi.routing.RoutingService;
import toools.SizeOf;

public class ChordService extends Service implements SizeOf {
	Set<Item> items = new HashSet<>();
	RoutingService r = component.defaultRoutingProtocol();

	public void store(Item item) {
		// async multicast the item to all targets
		r.exec(ChordService.class, set.class, r.defaultData(), ComponentMatcher.multicast(hash(item.key)), null, item);
	}

	public Item get(String key) {
		var hosts = hash(key);

		// try all components in a sequence
		for (var c : hosts) {
			// sync call
			var msg = r.exec(ChordService.class, get.class, r.defaultData(), ComponentMatcher.unicast(c), true,
					key).returnQ.poll_sync();

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

	public List<ItemLocation> search(String regex, double searchDuration) {
		// SYNC multicast the item to all targets
		return r.exec(ChordService.class, get.class, regex).returnQ.collector().collectDuring(searchDuration).messages
				.stream().filter(msg -> msg.content instanceof Item)
				.map(msg -> new ItemLocation((Item) msg.content, msg.route.source())).toList();
	}

	public Set<Component> hash(String key) {
		var activeLinks = component.localView().links().stream().filter(l -> l.isActive());
		var components = activeLinks.map(l -> l.dest.component).toList();

		int nbTargets = 2;

		var r = new HashSet<Component>();

		for (int i = 0; i < nbTargets; ++i) {
			// compute a hash for that target
			r.add(components.get((key.hashCode() + "/" + i).hashCode() % components.size()));
		}

		return r;
	}

	public class keys extends TypedInnerClassEndpoint {
		public Collection<String> keys(String regex) {
			return items.stream().map(i -> i.key).filter(key -> key.matches(regex)).toList();
		}

		@Override
		public String getDescription() {
			return "list the entries stored in this component";
		}
	}

	public class set extends TypedInnerClassEndpoint {
		public void f(Item i) {
			items.add(i);
		}

		@Override
		public String getDescription() {
			return "stores this items locally";
		}
	}

	public class get extends InnerClassEndpoint {

		@Override
		public void impl(MessageQueue in) throws Throwable {
			var execMsg = in.poll_sync();
			String re = (String) execMsg.content;
			items.stream().filter(i -> i.key.matches(re)).forEach(i -> reply(execMsg, i));
		}

		@Override
		public String getDescription() {
			return "retrieves all items matching the given name";
		}
	}

	public class deleteRegex extends TypedInnerClassEndpoint {
		public void delete(String regex) {
			items.removeIf(i -> i.key.matches(regex));
		}

		@Override
		public String getDescription() {
			return "stores this items locally";
		}
	}
}
