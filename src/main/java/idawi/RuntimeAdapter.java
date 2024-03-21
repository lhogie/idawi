package idawi;

public class RuntimeAdapter implements AgendaListener {

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
	public void newEventInThreadPool(Event<?> e) {
	}

	@Override
	public void sleeping(double waitingTime, Event<?> event) {
	}

	@Override
	public void interrupted() {
	}

	@Override
	public void sleeping(long waitTimeMs) {
	}

	@Override
	public void terminating(long nbPastEvents) {
	}

	@Override
	public void starting() {
	}

}
