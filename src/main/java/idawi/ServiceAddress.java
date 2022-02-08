package idawi;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Objects;

import toools.reflect.Clazz;

public class ServiceAddress implements Externalizable {
	private static final long serialVersionUID = 1L;
	public To to;
	public Class<? extends Service> service;

	public ServiceAddress() {

	}

	public ServiceAddress(To ca, Class<? extends Service> sid) {
		this.to = ca;
		this.service = sid;
	}

	public QueueAddress q(String name) {
		return new QueueAddress(this, name);
	}

	public OperationAddress o(String name) {
		return new OperationAddress(this, name);
	}

	public OperationAddress o(Class<? extends Operation> o) {
		return o(o.getName());
	}

	@Override
	public String toString() {
		return to.toString() + "/" + Clazz.classNameWithoutPackage(service.getName());
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
		return super.equals(o) && Objects.equals(service, t.service);
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(to);
		out.writeObject(service);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		to = (To) in.readObject();
		service = (Class) in.readObject();
	}

	public Class<? extends Service> getServiceID() {
		return service;
	}
}
