package idawi;

public interface RuntimeListener {
	void newEvent(Event<?> e);

	void eventSubmitted(Event<SpecificTime> newEvent);
}
