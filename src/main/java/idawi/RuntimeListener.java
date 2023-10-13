package idawi;

import java.io.PrintStream;

public interface RuntimeListener {

	void eventSubmitted(Event<?> newEvent);

	void eventProcessingStarts(Event<?> e);

	void eventProcessingCompleted(Event<?> e);

	void newEventScheduledForExecution(Event<?> e);

	void sleeping(long waitTimeMs);

	void terminating(long nbPastEvents);

	void sleeping(double waitTime, Event<?> event);

	void interrupted();

	public static class StdOutRuntimeListener implements RuntimeListener {

		private PrintStream out;

		public StdOutRuntimeListener(PrintStream out) {
			this.out = out;
		}


		private void print(String s) {
			RuntimeEngine.stdout(s);
		}

		@Override
		public void eventSubmitted(Event<?> newEvent) {
			print(" * new event submitted: " + newEvent);
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
			print(" * newEventScheduledForExecution: " + e);

		}

		@Override
		public void sleeping(double duration, Event<?> event) {
			print(" * waiting " + Utils.prettyTime(duration) + " to execute " + event);
		}

		@Override
		public void interrupted() {
			print(" * interrupted");
		}

		@Override
		public void sleeping(long waitTimeMs) {
			print(" * sleeping: " + waitTimeMs + "s");
		}

		@Override
		public void terminating(long nbPastEvents) {
			print(" * terminating: " + nbPastEvents + " events processed");
		}

		@Override
		public void starting() {
			print(" * starting");
		}

	}

	void starting();

}