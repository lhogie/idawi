package idawi.transport;

import java.util.ArrayList;

import idawi.Agenda;
import idawi.Idawi;
import toools.SizeOf;

public class Activity extends ArrayList<TimeFrame> implements SizeOf {
	public double totalTimeSpan() {
		return last().end() - get(0).start();
	}

	public double sumActivityDuration() {
		return stream().mapToDouble(p -> p.duration()).sum();
	}

	public double avgAvailabilityDuration() {
		return stream().mapToDouble(p -> p.duration()).average().getAsDouble();
	}

	public double activityRatio() {
		return sumActivityDuration() / totalTimeSpan();
	}

	@Override
	public long sizeOf() {
		return size() * 8;
	}

	public boolean availableAt(double time) {
		return stream().anyMatch(p -> p.includes(time));
	}

	public void merge(Activity a) {
		for (var f : a) {
			// TODO
		}
	}

	public void markActive() {
		if (isEmpty()) {
			add(new TimeFrame(Idawi.agenda.now()));
		} else {
			var last = last();

			if (last.isClosed()) {
				add(new TimeFrame(Idawi.agenda.now()));
			} else {
				last.end(Idawi.agenda.now());
			}
		}
	}


	public TimeFrame last() {
		return get(size() - 1);
	}

	public boolean available() {
		return isEmpty() ? false : !last().isClosed();
	}
}