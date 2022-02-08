package idawi.service.time;

import idawi.Component;
import idawi.TypedOperation;
import idawi.Service;

public class TimeService extends Service {

	TimeModel tm = new ComputerClockTimeModel();

	public TimeService(Component peer) {
		super(peer);
		registerOperation(new getTime());
		registerOperation(new setTime());
		registerOperation(new getModel());
	}

	public class getTime extends TypedOperation {
		public double f() {
			return tm.getTime();
		}

		@Override
		public String getDescription() {
			// TODO Auto-generated method stub
			return null;
		}
	}

	public class setTime extends TypedOperation {
		public void f(double newTime) {
			((SettableTimeModel) tm).time = newTime;
		}

		@Override
		public String getDescription() {
			// TODO Auto-generated method stub
			return null;
		}
	}

	public class getModel extends TypedOperation {
		public TimeModel f() {
			return tm;
		}

		@Override
		public String getDescription() {
			// TODO Auto-generated method stub
			return null;
		}
	}

}
