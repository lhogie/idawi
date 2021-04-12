package idawi;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class ComponentAddress implements Externalizable {
	private static final long serialVersionUID = 1L;

	public Set<ComponentDescriptor> notYetReachedExplicitRecipients;
	public int coverage;
	public double forwardProbability;
	public double validityDuration = 10;// Double.MAX_VALUE;

	public static final ComponentAddress BCAST_ADDRESS = new ComponentAddress();

	public ComponentAddress() {
		this(null, Integer.MAX_VALUE, 1);
	}

	public ComponentAddress(Set<ComponentDescriptor> peers, int maxDistance, double forwardProbability) {
		this.notYetReachedExplicitRecipients = peers;
		this.coverage = maxDistance;
		this.forwardProbability = forwardProbability;
	}

	public ComponentAddress(Set<ComponentDescriptor> peers) {
		this(peers, Integer.MAX_VALUE, 1);
	}

	public ComponentAddress(int maxDistance) {
		this(null, maxDistance, 1);
	}

	public static ComponentAddress to(Set<ComponentDescriptor> peers) {
		return new ComponentAddress(peers);
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
		return notYetReachedExplicitRecipients == null ? "*" : notYetReachedExplicitRecipients.toString();
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof ComponentAddress))
			return false;

		ComponentAddress t = (ComponentAddress) o;
		return Objects.equals(notYetReachedExplicitRecipients, t.notYetReachedExplicitRecipients)
				&& coverage == t.coverage;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {

		if (notYetReachedExplicitRecipients == null) {
			out.writeInt(-1);
		} else {
			out.writeInt(notYetReachedExplicitRecipients.size());

			for (ComponentDescriptor p : notYetReachedExplicitRecipients) {
				out.writeObject(p);
			}
		}

		out.writeInt(coverage);
		out.writeDouble(validityDuration);
		out.writeDouble(forwardProbability);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		int toLen = in.readInt();

		if (toLen != -1) {
			notYetReachedExplicitRecipients = new HashSet<ComponentDescriptor>(toLen);

			for (int i = 0; i < toLen; ++i) {
				notYetReachedExplicitRecipients.add((ComponentDescriptor) in.readObject());
			}
		}

		coverage = in.readInt();
		validityDuration = in.readDouble();
		forwardProbability = in.readDouble();
	}

	public boolean isBroadcast() {
		return notYetReachedExplicitRecipients == null;
	}

	public boolean isUnicast() {
		return notYetReachedExplicitRecipients != null && notYetReachedExplicitRecipients.size() == 1;
	}

	public boolean isMulticast() {
		return notYetReachedExplicitRecipients != null && notYetReachedExplicitRecipients.size() > 1;
	}

	public double getValidityDuration() {
		return validityDuration;
	}

	public Set<ComponentDescriptor> getNotYetReachedExplicitRecipients() {
		return notYetReachedExplicitRecipients;
	}

	public int getMaxDistance() {
		return coverage;
	}

	public double getForwardProbability() {
		return forwardProbability;
	}
}
