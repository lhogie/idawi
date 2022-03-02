package idawi;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import toools.reflect.Clazz;

public class ServiceAddress implements Externalizable {
	private static final long serialVersionUID = 1L;
	public To to;
	public String serviceName;

	public ServiceAddress() {

	}

	public ServiceAddress(To ca, Class<? extends Service> sid) {
		this.to = ca;
		this.serviceName = sid.getName();
	}

	public QueueAddress q(String name) {
		return new QueueAddress(this, name);
	}

	public OperationAddress o(String name) {
		return new OperationAddress(this, name);
	}

	public OperationAddress o(Class<? extends InnerOperation> o) {
		return o(InnerOperation.name(o));
	}

	@Override
	public String toString() {
		return to.toString() + "/" + Clazz.classNameWithoutPackage(serviceName);
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof ServiceAddress))
			return false;

		ServiceAddress t = (ServiceAddress) o;
		return super.equals(o) && serviceName.equals(t.serviceName);
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(to);
		out.writeUTF(serviceName);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		to = (To) in.readObject();
		serviceName = in.readUTF();
	}

	public Class<? extends Service> getServiceID() {
		return Clazz.findClass(serviceName);
	}
}
