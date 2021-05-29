package idawi;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Objects;
import java.util.Set;

import idawi.AsMethodOperation.OperationID;
import toools.reflect.Clazz;
import toools.util.Date;

public class QueueAddress extends ServiceAddress {
	private static final long serialVersionUID = 1L;

	public String queue;

	public QueueAddress() {

	}

	public QueueAddress(Set<ComponentDescriptor> peers, Class<? extends Service> sid, String qid, int maxDistance,
			double forwardProbability) {
		super(peers, sid, maxDistance, forwardProbability);
		this.queue = qid;
	}

	public QueueAddress(Set<ComponentDescriptor> peers, OperationID operation, int maxDistance,
			double forwardProbability) {
		this(peers, operation.declaringService, operation.operationName + "@" + Date.time(), maxDistance, forwardProbability);
	}

	public QueueAddress(Set<ComponentDescriptor> peers, Class<? extends Service> sid, String qid) {
		this(peers, sid, qid, Integer.MAX_VALUE, 1);
	}

	public static QueueAddress to(Set<ComponentDescriptor> peers, Class<? extends Service> sid, String qid) {
		return new QueueAddress(peers, sid, qid);
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
	public QueueAddress(Set<ComponentDescriptor> r, OperationID c) {
		this(r, c.declaringService, c.operationName);
	}

	public QueueAddress(OperationID c) {
		this((Set) null, c.declaringService, c.operationName);
	}

	public QueueAddress(ComponentDescriptor t, OperationID c) {
		this(Set.of(Objects.requireNonNull(t)), c);
	}

	private static String innerClassName(Class c) {
		var ec = c.getEnclosingClass();

		if (ec == null)
			throw new IllegalArgumentException(c + " is not an inner class");

		return c.getName().substring(ec.getName().length() + 1);
	}

	private static Class enclosingClass(Object lambda) {
		int i = lambda.getClass().getName().indexOf("$$Lambda$");

		if (i < 0) {
			throw new IllegalStateException("this is not a lambda");
		}

		return Clazz.findClass(lambda.getClass().getName().substring(0, i));
	}

	@Override
	public String toString() {
		return super.toString() + "->" + queue;
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof QueueAddress))
			return false;

		QueueAddress t = (QueueAddress) o;
		return super.equals(o) && Objects.equals(queue, t.queue);
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		out.writeObject(queue);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		queue = (String) in.readObject();
	}

}
