package idawi.service.time;

import idawi.Component;
import idawi.Service;
import idawi.TypedInnerClassEndpoint;

public class TimeService extends Service {
	public TimeModel model = new ComputerClockTimeModel();

	public TimeService(Component peer) {
		super(peer);
	}

	public class getTime extends TypedInnerClassEndpoint {
		public Time f() {
			return now2();
		}

		@Override
		public String getDescription() {
			return "gets the current time";
		}
	}

	public class getModel extends TypedInnerClassEndpoint {
		public TimeModel f() {
			return model;
		}

		@Override
		public String getDescription() {
			return "get the time model";
		}
	}

	public Time now2() {
		return new Time(model.getTime(), model);
	}

}
