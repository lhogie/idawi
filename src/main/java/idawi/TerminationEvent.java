package idawi;

public class TerminationEvent extends Event<PointInTime> {

	public TerminationEvent(double d) {
		super("termination", new PointInTime(d));
	}

	@Override
	public void run() {
		RuntimeEngine.terminationRequired.set(true);
	}
}
