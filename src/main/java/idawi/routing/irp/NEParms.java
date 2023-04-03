package idawi.routing.irp;

import java.util.Objects;
import java.util.Set;

import idawi.Component;
import idawi.routing.RoutingData;
import idawi.routing.RoutingService;
import toools.SizeOf;
import toools.io.Utilities;

public class NEParms extends RoutingData {
	private static final long serialVersionUID = 1L;

	public Set<Component> componentNames;
	public int coverage;
	public double validityDuration = 10;// Double.MAX_VALUE;

	@Override
	public long sizeOf() {
		return 8 + SizeOf.sizeOf(componentNames);
	}

	public NEParms() {
		this(null, Integer.MAX_VALUE);
	}

	public NEParms(Set<Component> peers, int maxDistance) {
		this.componentNames = peers;
		this.coverage = maxDistance;
	}

	public NEParms(Component p) {
		this(Set.of(p));
	}

	public NEParms(Set<Component> peers) {
		this(peers, Integer.MAX_VALUE);
	}

	public NEParms(int maxDistance) {
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
	public String toString() {
		return componentNames == null ? "*" : componentNames.toString();
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof NEParms))
			return false;

		NEParms t = (NEParms) o;
		return Objects.equals(componentNames, t.componentNames) && coverage == t.coverage;
	}

	public boolean isBroadcast() {
		return componentNames == null;
	}

	public boolean isUnicast() {
		return componentNames != null && componentNames.size() == 1;
	}

	public boolean isMulticast() {
		return componentNames != null && componentNames.size() > 1;
	}

	public double getValidityDuration() {
		return validityDuration;
	}

	public Set<Component> getNotYetReachedExplicitRecipients() {
		return componentNames;
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
			componentNames = null;
		} else {
			for (var n : names.split(" *, *")) {
				componentNames.add(service.component.digitalTwinService().lookup(n));
			}
		}
	}
}
