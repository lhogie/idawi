package idawi;

public class StdOutRuntimeListener implements RuntimeListener {

	@Override
	public void newEvent(Event<?> e) {
		System.out.println("new event picked up: " + e);
	}

	@Override
	public void eventSubmitted(Event<SpecificTime> newEvent) {
		System.out.println("new event submitted: " + newEvent);
	}
}
