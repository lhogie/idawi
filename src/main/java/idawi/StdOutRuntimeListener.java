package idawi;

public class StdOutRuntimeListener implements RuntimeListener {

	private void print(String s) {
		System.out.println(String.format("%.2f", RuntimeEngine.now())+ "\t" + s);
	}
	
	@Override
	public void newEventTakenFromQueue(Event<?> e) {
		print(RuntimeEngine.now()+ "\t" + "new event picked up: " + e);
	}

	@Override
	public void eventSubmitted(Event<?> newEvent) {
		print("new event submitted: " + newEvent);
	}

	@Override
	public void eventProcessingStarts(Event<?> e) {
		print(" * eventProcessingStarts: " + e);
	}

	@Override
	public void eventProcessingCompleted(Event<?> e) {
		print(" * eventProcessingCompleted: " + e);
	}

	@Override
	public void newEventScheduledForExecution(Event<?> e) {
		print("newEventScheduledForExecution: " + e);

	}

	@Override
	public void waitingForEvent() {
		print("waitingForEvent: ");
	}

	@Override
	public void interrupted() {
		print("interrupted");
	}

	@Override
	public void sleeping(long waitTimeMs) {
		print("sleeping: " + waitTimeMs);
	}

	@Override
	public void terminating(long nbPastEvents) {
		print("terminating: " + nbPastEvents + " events processed");
	}

}
