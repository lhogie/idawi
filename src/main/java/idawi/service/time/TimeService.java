package idawi.service.time;

import idawi.Component;
import idawi.InnerClassTypedOperation;
import idawi.Service;

public class TimeService extends Service {

	TimeModel tm = new ComputerClockTimeModel();

	public TimeService(Component peer) {
		super(peer);
	}

	public class getTime extends InnerClassTypedOperation {
		public double f() {
			return tm.getTime();
		}

		@Override
		public String getDescription() {
			// TODO Auto-generated method stub
			return null;
		}
	}

	public class setTime extends InnerClassTypedOperation {
		public void f(double newTime) {
			((SettableTimeModel) tm).time = newTime;
		}

		@Override
		public String getDescription() {
			// TODO Auto-generated method stub
			return null;
		}
	}

	public class getModel extends InnerClassTypedOperation {
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
