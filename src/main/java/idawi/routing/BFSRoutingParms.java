package idawi.routing;

import java.util.HashSet;
import java.util.Set;

import idawi.Component;
import toools.SizeOf;
import toools.text.TextUtilities;

public class BFSRoutingParms extends RoutingData {
	private static final long serialVersionUID = 1L;

	Set<Component> recipients;

	@Override
	public void fromString(String s, RoutingService service) {
		recipients = new HashSet<>();

		for (var n : s.split(" *, *")) {
			var c = service.component.localView().g.findComponentByFriendlyName(n);

			if (c != null) {
				recipients.add(c);
			}
		}
	}

	@Override
	public long sizeOf() {
		return 8 + SizeOf.sizeOf(recipients);
	}

	@Override
	public String toURLElement() {
		return TextUtilities.concat(" ", recipients);
	}
}