package idawi;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Objects;

import idawi.AsMethodOperation.OperationID;
import toools.reflect.Clazz;

public class ServiceAddress implements Externalizable {
	private static final long serialVersionUID = 1L;
	public ComponentAddress componentAddress;
	public Class<? extends Service> service;

	public ServiceAddress() {

	}

	public ServiceAddress(ComponentAddress ca, Class<? extends Service> sid) {
		this.componentAddress = ca;
		this.service = sid;
	}

	public QueueAddress q(String name) {
		return new QueueAddress(this, name);
	}
	
	public OperationAddress o(String name) {
		return new OperationAddress(this, name);
	}

	public OperationAddress o(OperationID o) {
		return o(o.operationName);
	}

	
	@Override
	public String toString() {
		return super.toString() + "->" + Clazz.classNameWithoutPackage(service.getName());
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
		out.writeObject(componentAddress);
		out.writeObject(service);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		componentAddress = (ComponentAddress) in.readObject();
		service = (Class) in.readObject();
	}

	public Class<? extends Service> getServiceID() {
		return service;
	}
}
