package idawi.routing;

import idawi.InnerClassEndpoint;
import idawi.Service;

public class MessageODestination extends Destination {
	public Class<? extends InnerClassEndpoint> operationID;
	Class<? extends Service> service;
	public boolean premptive;
	public long invocationDate;
	public long instanceID;

	public MessageODestination(Class<? extends Service> service, Class<? extends InnerClassEndpoint> operationID) {
		if (service == null)
			throw new NullPointerException(operationID + " has no service");

		this.operationID = operationID;
	}

	@Override
	public long sizeOf() {
		return super.sizeOf() + 8 + 8 + 1 + 8 + 8;
	}

	@Override
	public Class<? extends Service> service() {
		return service;
	}

	public MessageQDestination m() {
		var d = new MessageQDestination();
		d.componentMatcher = componentMatcher;
		d.service = service();
		d.queueID = queueID();
		return d;
	}

	@Override
	public String queueID() {
		return operationID.getSimpleName();// + "@" + invocationDate;
	}

	@Override
	public String toString() {
		return "to endpoint " + super.toString();
	}

}