package idawi.routing;

import idawi.InnerClassOperation;
import idawi.Service;

public class MessageODestination extends Destination {
	public Class<? extends InnerClassOperation> operationID;
	public boolean premptive;
	public long invocationDate;
	public long instanceID;


	@Override
	public Class<? extends Service> service() {
		return InnerClassOperation.serviceClass(operationID);
	}

	public MessageQDestination m() {
		var d = new MessageQDestination();
		d.componentTarget = componentTarget;
		d.service = service();
		d.queueID = queueID();
		return d;
	}

	@Override
	public String queueID() {
		return InnerClassOperation.name(operationID) + "@" + invocationDate;
	}

	@Override
	public String toString() {
		return "to operation " + super.toString();
	}
}