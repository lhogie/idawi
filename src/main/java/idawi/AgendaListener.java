package idawi;

import java.io.PrintStream;

import toools.util.Date;

public interface AgendaListener {

	void eventSubmitted(Event<?> newEvent);

	void eventProcessingStarts(Event<?> e);

	void eventProcessingCompleted(Event<?> e);

	void newEventInThreadPool(Event<?> e);

	void sleeping(long waitTimeMs);

	void terminating(long nbPastEvents);

	void sleeping(double waitTime, Event<?> event);

	void interrupted();

	public static class PrintStreamRuntimeListener implements AgendaListener {

		private PrintStream out;

		public PrintStreamRuntimeListener(PrintStream out) {
			this.out = out;
		}


		private void print(String s) {
			IO.stdout(s);
		}

		@Override
		public void eventSubmitted(Event<?> newEvent) {
			print(" @ new event submitted: " + newEvent);
		}

		@Override
		public void eventProcessingStarts(Event<?> e) {
			print(" @ eventProcessingStarts: " + e);
		}

		@Override
		public void eventProcessingCompleted(Event<?> e) {
			print(" @ eventProcessingCompleted: " + e);
		}

		@Override
		public void newEventInThreadPool(Event<?> e) {
			print(" @ newEventScheduledForExecution: " + e);

		}

		@Override
		public void sleeping(double duration, Event<?> event) {
			print(" @ waiting " + Date.prettyTime(duration) + " to execute " + event);
		}

		@Override
		public void interrupted() {
			print(" @ interrupted");
		}

		@Override
		public void sleeping(long waitTimeMs) {
			print(" @ sleeping: " + waitTimeMs + "s");
		}

		@Override
		public void terminating(long nbPastEvents) {
			print(" @ terminating: " + nbPastEvents + " events processed");
		}

		@Override
		public void starting() {
			print(" @ starting");
		}

	}

	void starting();

}
