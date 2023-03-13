package idawi.knowledge_base;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

import fr.cnrs.i3s.Cache;
import idawi.Component;
import idawi.TypedInnerClassOperation;
import idawi.knowledge_base.BFS.BFSResult;
import idawi.knowledge_base.info.DirectedLink;
import idawi.routing.Emission;
import idawi.routing.Reception;
import idawi.routing.Route;
import idawi.transport.TransportService;

public class MapService extends KnowledgeBase {

	public final NetworkMap map = new NetworkMap();
	public final Cache<BFSResult> bfsResult = new Cache<>(5,
			() -> BFS.bfs(map, component.ref(), 10, Integer.MAX_VALUE));

	List<Info> infos = new ArrayList<>();

	public MapService(Component component) {
		super(component);

		registerOperation(new get());
		registerOperation(new addLink());
		registerOperation(new clear());
		registerOperation(new size());
	}

	public void bidi(ComponentRef a, ComponentRef b, Class<? extends TransportService> tranport) {
		map.add(new DirectedLink(component.now(), a, tranport, b));
		map.add(new DirectedLink(component.now(), b, tranport, a));
	}

	@Override
	public void removeOutdated(double now) {
		map.removeOutdated(now);
	}

	private static void removeOutdate(double now, Iterator<? extends Info> i) {
		while (i.hasNext()) {
			var n = i.next();

			if (n.reliability(now) < 0.1) {
				i.remove();
			}
		}
	}

	public class get extends TypedInnerClassOperation {
		public NetworkMap get() {
			return map;
		}

		@Override
		public String getDescription() {
			return "provides a map of the network";
		}
	}

	@Override
	public String getFriendlyName() {
		return "topology information";
	}



	public void neighborLeft(ComponentRef c, TransportService transport, ComponentRef neighbor) {
		map.remove(c, transport.getClass(), neighbor);
		map.remove(neighbor, transport.getClass(), c);
	}

	public class addLink extends TypedInnerClassOperation {
		@Override
		public String getDescription() {
			return "accepts a new link info";
		}

		public void f(Collection<DirectedLink> i) {
			i.forEach(ii -> map.add(ii));
		}
	}

	public class size extends TypedInnerClassOperation {
		public int size() {
			return map.links.size();
		}

		@Override
		public String getDescription() {
			return "nb of links";
		}
	}

	public class clear extends TypedInnerClassOperation {
		public void f() {
			map.clear();
		}

		@Override
		public String getDescription() {
			return "clear the local view";
		}
	}



	public Set<ComponentRef> lookupByRegexp(String re) {
		var r = new HashSet<ComponentRef>();

		for (var e : map.components) {
			if (e.ref.matches(re)) {
				r.add(e);
			}
		}

		return r;
	}

	public void forEachInfo(Predicate<Info> p) {
		for (var i : map.links) {
			if (p.test(i)) {
				return;
			}
		}
	}

	public double avgReliability(double now) {
		class A {
			double sum;
			long count = 0;
		}

		var a = new A();

		forEachInfo(i -> {
			a.sum += i.reliability(now);
			++a.count;
			return false;
		});

		return a.sum / a.count;
	}

}
