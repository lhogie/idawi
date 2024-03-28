package idawi.routing;

import java.util.ArrayList;
import java.util.List;

import idawi.Component;
import toools.SizeOf;
import toools.text.TextUtilities;

public class IRTo extends RoutingParameters {
	List<Component> route = new ArrayList<>();

	@Override
	public void fromString(String s, RoutingService r) {
		route = new ArrayList<>();

		for (var n : s.split(" *, *")) {
			route.add(r.component.localView().g.findComponent(c -> c.friendlyName.equals(n), true, () -> {
				var c = new Component();
				c.friendlyName = n;
				return c;
			}));
		}
	}

	@Override
	public long sizeOf() {
		return 8 + SizeOf.sizeOf(route);
	}

	@Override
	public String toURLElement() {
		return TextUtilities.concat(" ", route);
	}
}