package idawi.service.time;

import idawi.AsMethodOperation.OperationID;
import idawi.Component;
import idawi.IdawiExposed;
import idawi.MessageQueue;
import idawi.Service;

public class TimeService extends Service {

	TimeModel tm = new ComputerClockTimeModel();

	public TimeService(Component peer) {
		super(peer);
	}

	public static OperationID getTime;

	@IdawiExposed
	public void getTime(MessageQueue in) {
		var trigger = in.get_blocking();
		send(tm.getTime(), trigger.requester);
	}

	public static OperationID getModel;

	@IdawiExposed
	public void getModel(MessageQueue in) {
		var trigger = in.get_blocking();
		send(tm, trigger.requester);
	}

	@IdawiExposed
	public void setTime(MessageQueue in) {
		var trigger = in.get_blocking();
		double newTime = (double) trigger.content;
		((ControlledTimeModel) tm).time = newTime;
	}
}
