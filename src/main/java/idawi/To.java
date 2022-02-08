package idawi;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class To implements Externalizable {
	private static final long serialVersionUID = 1L;

	public Set<ComponentDescriptor> componentIDs;
	public int coverage;
	public double forwardProbability;
	public double validityDuration = 10;// Double.MAX_VALUE;

	public static final To BCAST_ADDRESS = new To();

	public To() {
		this(null, Integer.MAX_VALUE, 1);
	}

	public To(Set<ComponentDescriptor> peers, int maxDistance, double forwardProbability) {
		this.componentIDs = peers;
		this.coverage = maxDistance;
		this.forwardProbability = forwardProbability;
	}

	public To(Component p) {
		this(p.descriptor());
	}

	public To(ComponentDescriptor p) {
		this(Set.of(p));
	}

	public To(Set<ComponentDescriptor> peers) {
		this(peers, Integer.MAX_VALUE, 1);
	}

	public To(int maxDistance) {
		this(null, maxDistance, 1);
	}

	public static To to(Set<ComponentDescriptor> peers) {
		return new To(peers);
	}

	public ServiceAddress s(Class<? extends Service> sid) {
		return new ServiceAddress(this, sid);
	}

	public <O extends InnerOperation> OperationAddress o(Class<O> o) {
		return new OperationAddress(this, o);
	}

	public <O extends InnerOperation> OperationAddress o(Class<? extends Service> sid, String operationName) {
		return s(sid).o(operationName);
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
		return componentIDs == null ? "*" : componentIDs.toString();
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof To))
			return false;

		To t = (To) o;
		return Objects.equals(componentIDs, t.componentIDs) && coverage == t.coverage;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {

		if (componentIDs == null) {
			out.writeInt(-1);
		} else {
			out.writeInt(componentIDs.size());

			for (ComponentDescriptor p : componentIDs) {
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
			componentIDs = new HashSet<ComponentDescriptor>(toLen);

			for (int i = 0; i < toLen; ++i) {
				componentIDs.add((ComponentDescriptor) in.readObject());
			}
		}

		coverage = in.readInt();
		validityDuration = in.readDouble();
		forwardProbability = in.readDouble();
	}

	public boolean isBroadcast() {
		return componentIDs == null;
	}

	public boolean isUnicast() {
		return componentIDs != null && componentIDs.size() == 1;
	}

	public boolean isMulticast() {
		return componentIDs != null && componentIDs.size() > 1;
	}

	public double getValidityDuration() {
		return validityDuration;
	}

	public Set<ComponentDescriptor> getNotYetReachedExplicitRecipients() {
		return componentIDs;
	}

	public int getMaxDistance() {
		return coverage;
	}

	public double getForwardProbability() {
		return forwardProbability;
	}
}
