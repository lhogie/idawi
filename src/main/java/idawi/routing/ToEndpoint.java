package idawi.routing;

import idawi.InnerClassEndpoint;
import idawi.Service;

public class ToEndpoint extends Destination {
	public Class<? extends InnerClassEndpoint> endpointID;
	Class<? extends Service> service;
	public boolean premptive;
	public long invocationDate;
	public long instanceID;

	public ToEndpoint(Class<? extends Service> service, Class<? extends InnerClassEndpoint> operationID) {
		if (service == null)
			throw new NullPointerException(operationID + " has no service");

		this.endpointID = operationID;
	}

	@Override
	public long sizeOf() {
		return super.sizeOf() + 8 + 8 + 1 + 8 + 8;
	}

	@Override
	public Class<? extends Service> service() {
		return service;
	}

	public ToQueue m() {
		var d = new ToQueue();
		d.componentMatcher = componentMatcher;
		d.service = service();
		d.queueID = queueID();
		return d;
	}

	@Override
	public String queueID() {
		return endpointID.getSimpleName();// + "@" + invocationDate;
	}

	@Override
	public String toString() {
		return "to endpoint " + super.toString();
	}

}