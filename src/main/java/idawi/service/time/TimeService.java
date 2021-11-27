package idawi.service.time;

import idawi.AsMethodOperation.OperationID;
import idawi.Component;
import idawi.IdawiOperation;
import idawi.MessageQueue;
import idawi.Service;

public class TimeService extends Service {

	TimeModel tm = new ComputerClockTimeModel();

	public TimeService(Component peer) {
		super(peer);
	}

	public static OperationID getTime;

	@IdawiOperation
	public void getTime(MessageQueue in) {
		var trigger = in.get_blocking();
		send(tm.getTime(), trigger.replyTo);
	}

	public static OperationID getModel;

	@IdawiOperation
	public void getModel(MessageQueue in) {
		var trigger = in.get_blocking();
		send(tm, trigger.replyTo);
	}

	@IdawiOperation
	public void setTime(MessageQueue in) {
		var trigger = in.get_blocking();
		double newTime = (double) trigger.content;
		((ControlledTimeModel) tm).time = newTime;
	}
}
