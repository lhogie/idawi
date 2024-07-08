package idawi.routing;

import java.util.HashSet;
import java.util.Set;

import idawi.Component;
import idawi.service.local_view.BFS.RRoute;
import toools.SizeOf;
import toools.text.TextUtilities;

public class BFSRoutingParms extends RoutingParameters {
	private static final long serialVersionUID = 1L;

	public Set<RRoute> paths;

	@Override
	public void fromString(String s, RoutingService service) {
	}

	@Override
	public long sizeOf() {
		return 8 + SizeOf.sizeOf(paths);
	}

	@Override
	public String toURLElement() {
		return TextUtilities.concat(" ", paths);
	}
}