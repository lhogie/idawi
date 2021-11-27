package idawi;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Objects;

import toools.reflect.Clazz;

public class QueueAddress implements Externalizable {

	private static final long serialVersionUID = 1L;
	public ServiceAddress serviceAddress;
	public String queue;

	public QueueAddress() {

	}

	public QueueAddress(ServiceAddress s, String qid) {
		this.serviceAddress = s;
		this.queue = qid;
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
		out.writeObject(serviceAddress);
		out.writeObject(queue);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		serviceAddress = (ServiceAddress) in.readObject();
		queue = (String) in.readObject();
	}

}
