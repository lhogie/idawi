package idawi.routing;

import java.util.ArrayList;
import java.util.List;

import idawi.Component;
import toools.SizeOf;
import toools.text.TextUtilities;

public class IRTo extends RoutingData {
	List<Component> route = new ArrayList<>();

	@Override
	public void fromString(String s, RoutingService r) {
		route = new ArrayList<>();

		for (var n : s.split(" *, *")) {
			route.add(r.component.localView().g.findComponentByFriendlyName(n));
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