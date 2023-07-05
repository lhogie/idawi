package idawi;

public class RuntimeAdapter implements RuntimeListener {

	@Override
	public void newEventTakenFromQueue(Event<?> e) {
	}

	@Override
	public void eventSubmitted(Event<?> newEvent) {
	}

	@Override
	public void eventProcessingStarts(Event<?> e) {
	}

	@Override
	public void eventProcessingCompleted(Event<?> e) {
	}

	@Override
	public void newEventScheduledForExecution(Event<?> e) {
	}

	@Override
	public void waitingForEvent() {
	}

	@Override
	public void interrupted() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sleeping(long waitTimeMs) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void terminating(long nbPastEvents) {
		// TODO Auto-generated method stub
		
	}

}
