package idawi;

public interface RuntimeListener {
	void newEventTakenFromQueue(Event<?> e);

	void eventSubmitted(Event<?> newEvent);

	void eventProcessingStarts(Event<?> e);

	void eventProcessingCompleted(Event<?> e);

	void newEventScheduledForExecution(Event<?> e);

	void waitingForEvent();

	void interrupted();

	void sleeping(long waitTimeMs);

	void terminating(long nbPastEvents);
}
