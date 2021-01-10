package idawi;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import toools.reflect.Clazz;

public class To implements Externalizable {
	private static final long serialVersionUID = 1L;

	public Set<ComponentDescriptor> notYetReachedExplicitRecipients;
	public Class<? extends Service> service;
	public String operationOrQueue;
	public int coverage;
	public double forwardProbability;
	public double validityDuration = 10;// Double.MAX_VALUE;

	public To() {

	}

	public To(Set<ComponentDescriptor> peers, Class<? extends Service> sid, String qid, int maxDistance,
			double forwardProbability) {
		this.notYetReachedExplicitRecipients = peers;
		this.service = sid;
		this.operationOrQueue = qid;
		this.coverage = maxDistance;
		this.forwardProbability = forwardProbability;
	}

	public To(Set<ComponentDescriptor> peers, Class<? extends Service> sid, String qid) {
		this(peers, sid, qid, Integer.MAX_VALUE, 1);
	}

	public To(ComponentDescriptor t, Class<? extends Service> sid, String qid) {
		this(Set.of(t), sid, qid);
	}

	public To(Component c, Class<? extends Service> sid, String qid) {
		this(c.descriptor(), sid, qid);
	}

	public To(Class<? extends Service> sid, String qid) {
		this((Set) null, sid, qid);
	}

	public To(Set<ComponentDescriptor> s, Class<? extends Service> sid, AAA operationID) {
		this(s, sid, operationID.operation.getName());
	}

	public To(ComponentDescriptor c, Class<? extends Service> sid, AAA operationID) {
		this(c, sid, operationID.operation.getName());
	}

	private Class enclosingClass(Object lambda) {
		int i = lambda.getClass().getName().indexOf("$$Lambda$");

		if (i < 0) {
			throw new IllegalStateException("this is not a lambda");
		}

		return Clazz.findClass(lambda.getClass().getName().substring(0, i));
	}

	@Override
	public String toString() {
		return (notYetReachedExplicitRecipients == null ? "*" : notYetReachedExplicitRecipients) + "->"
				+ Clazz.classNameWithoutPackage(service.getName()) + "->" + operationOrQueue;
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
		return Objects.equals(notYetReachedExplicitRecipients, t.notYetReachedExplicitRecipients)
				&& Objects.equals(operationOrQueue, t.operationOrQueue) && Objects.equals(service, t.service)
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

		out.writeObject(service);
		out.writeObject(operationOrQueue);
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

		service = (Class) in.readObject();
		operationOrQueue = (String) in.readObject();
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
}
