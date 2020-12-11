package idawi;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashSet;
import java.util.Set;

import toools.reflect.Clazz;

public class To implements Externalizable {
	private static final long serialVersionUID = 1L;

	public Set<ComponentInfo> notYetReachedExplicitRecipients;
	public Class<? extends Service> service;
	public String operationOrQueue;
	public int coverage = Integer.MAX_VALUE;

	// valid 1s by default
	public double validityDuration = 1;

	public To() {

	}

	public To(Set<ComponentInfo> peers, Class<? extends Service> sid, String qid) {
		this.notYetReachedExplicitRecipients = peers;
		this.service = sid;
		this.operationOrQueue = qid;
	}

	public To(ComponentInfo t, Class<? extends Service> sid, String qid) {
		this(Set.of(t), sid, qid);
	}
	
	public To(Class<? extends Service> sid, String qid) {
		this((Set) null, sid, qid);
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
		return Utils.equals(notYetReachedExplicitRecipients, t.notYetReachedExplicitRecipients)
				&& Utils.equals(operationOrQueue, t.operationOrQueue) && Utils.equals(service, t.service)
				&& coverage == t.coverage;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {

		if (notYetReachedExplicitRecipients == null) {
			out.writeInt(-1);
		} else {
			out.writeInt(notYetReachedExplicitRecipients.size());

			for (ComponentInfo p : notYetReachedExplicitRecipients) {
				out.writeObject(p);
			}
		}

		out.writeObject(service);
		out.writeObject(operationOrQueue);
		out.writeInt(coverage);
		out.writeDouble(validityDuration);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		int toLen = in.readInt();

		if (toLen != -1) {
			notYetReachedExplicitRecipients = new HashSet<ComponentInfo>(toLen);

			for (int i = 0; i < toLen; ++i) {
				notYetReachedExplicitRecipients.add((ComponentInfo) in.readObject());
			}
		}

		service = (Class) in.readObject();
		operationOrQueue = (String) in.readObject();
		coverage = in.readInt();
		validityDuration = in.readDouble();
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
