package idawi.routing;

import java.util.HashSet;
import java.util.Set;

import idawi.Component;
import toools.SizeOf;

public class BFSRoutingParms extends RoutingData {
	private static final long serialVersionUID = 1L;

	Set<Component> recipients;

	@Override
	public void fromString(String s, RoutingService service) {
		recipients = new HashSet<>();

		for (var n : s.split(" *, *")) {
			var c = service.component.digitalTwinService().lookup(n);

			if (c != null) {
				recipients.add(c);
			}
		}
	}

	@Override
	public long sizeOf() {
		return 8 + SizeOf.sizeOf(recipients);
	}
}