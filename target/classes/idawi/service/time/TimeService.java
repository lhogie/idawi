package idawi.service.time;

import idawi.Component;
import idawi.Service;
import idawi.TypedInnerClassOperation;
import idawi.messaging.Message;
import idawi.routing.BlindBroadcasting;

public class TimeService extends Service {
	public TimeModel model = new ComputerClockTimeModel();

	public TimeService(Component peer) {
		super(peer);
		registerOperation(new getTime());
		registerOperation(new setTime());
		registerOperation(new getModel());
	}

	public class getTime extends TypedInnerClassOperation {
		public Time f() {
			return now2();
		}

		@Override
		public String getDescription() {
			return "gets the current time";
		}
	}

	public class setTime extends TypedInnerClassOperation {
		public void f(double newTime, boolean bcast) {
			if (!(model instanceof SettableTimeModel))
				throw new IllegalStateException("use a settable time model");

			((SettableTimeModel) model).setTime(newTime);

			if (bcast) {
				component.bb().send(now2(), null);
			}
		}

		@Override
		public String getDescription() {
			return "force the time in this component";
		}
	}

	public class getModel extends TypedInnerClassOperation {
		public TimeModel f() {
			return model;
		}

		@Override
		public String getDescription() {
			return "get the time model";
		}
	}

	public double now() {
		return model.getTime();
	}

	public Time now2() {
		return new Time(model.getTime(), model);
	}

}
