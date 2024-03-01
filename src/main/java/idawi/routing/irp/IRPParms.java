package idawi.routing.irp;

import java.util.Set;

import idawi.Component;
import idawi.routing.RoutingData;
import idawi.routing.RoutingService;
import toools.SizeOf;
import toools.io.Utilities;
import toools.text.TextUtilities;

public class IRPParms extends RoutingData {
	private static final long serialVersionUID = 1L;

	public Set<Component> components;
	public int coverage = 10;
	public double validityDuration = 10;// Double.MAX_VALUE;

	@Override
	public long sizeOf() {
		return 8 + SizeOf.sizeOf(components);
	}

	public IRPParms() {
		this(null, Integer.MAX_VALUE);
	}

	public IRPParms(Set<Component> peers, int maxDistance) {
		this.components = peers;
		this.coverage = maxDistance;
	}

	public IRPParms(Component p) {
		this(Set.of(p));
	}

	public IRPParms(Set<Component> peers) {
		this(peers, Integer.MAX_VALUE);
	}

	public IRPParms(int maxDistance) {
		this(null, maxDistance);
	}

	/*
	 * public To(ComponentDescriptor t, Class<? extends Service> sid, String qid) {
	 * this(Set.of(Objects.requireNonNull(t)), sid, qid); }
	 * 
	 * public To(Component c, Class<? extends Service> sid, String qid) {
	 * this(c.descriptor(), sid, qid); }
	 * 
	 * public To(Class<? extends Service> sid, String qid) { this((Set) null, sid,
	 * qid); }
	 */
	/*
	 * public To(ComponentDescriptor t, Class<? extends InInnerClassOperation> c) {
	 * this(Objects.requireNonNull(t), (Class<? extends Service>)
	 * c.getEnclosingClass(), innerClassName(c)); }
	 * 
	 * public To(Set<ComponentDescriptor> r, Class<? extends InInnerClassOperation>
	 * c) { this(r, (Class<? extends Service>) c.getEnclosingClass(),
	 * innerClassName(c)); }
	 */

	@Override
	public String toURLElement() {
		return "d=" + coverage + ",v=" + validityDuration + ",c="
				+ (components == null ? "" : TextUtilities.concat(" ", components));
	}

	public boolean isBroadcast() {
		return components == null;
	}

	public boolean isUnicast() {
		return components != null && components.size() == 1;
	}

	public boolean isMulticast() {
		return components != null && components.size() > 1;
	}

	public double getValidityDuration() {
		return validityDuration;
	}

	public Set<Component> getNotYetReachedExplicitRecipients() {
		return components;
	}

	public int getMaxDistance() {
		return coverage;
	}

	@Override
	public void fromString(String s, RoutingService service) {
		var m = Utilities.csv2map(s);
		coverage = Integer.valueOf(m.get("coverage"));
		validityDuration = Double.valueOf(m.get("validityDuration"));
		var names = m.get("names");

		if (names == null) {
			components = null;
		} else {
			for (var n : names.split(" *, *")) {
				components.add(service.component.localView().g.findComponentByFriendlyName(n));
			}
		}
	}

}
