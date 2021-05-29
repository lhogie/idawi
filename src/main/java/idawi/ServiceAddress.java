package idawi;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Objects;
import java.util.Set;

import toools.reflect.Clazz;

public class ServiceAddress extends ComponentAddress {
	private static final long serialVersionUID = 1L;

	public Class<? extends Service> service;

	public ServiceAddress() {

	}

	public ServiceAddress(Set<ComponentDescriptor> peers, Class<? extends Service> sid, int maxDistance,
			double forwardProbability) {
		super(peers, maxDistance, forwardProbability);
		this.service = sid;
	}

	public ServiceAddress(ComponentAddress components, Class<? extends Service> sid) {
		this(components.notYetReachedExplicitRecipients, sid, components.coverage, components.forwardProbability);
	}

	public ServiceAddress(Set<ComponentDescriptor> peers, Class<? extends Service> sid) {
		this(peers, sid, Integer.MAX_VALUE, 1);
	}

	public static ServiceAddress to(Set<ComponentDescriptor> peers, Class<? extends Service> sid) {
		return new ServiceAddress(peers, sid);
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
		super.writeExternal(out);
		out.writeObject(service);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		service = (Class) in.readObject();
	}

	public Class<? extends Service> getServiceID() {
		return service;
	}
}
