package idawi;

import java.io.Serializable;

public abstract class Event<W extends When> implements Runnable, Serializable {
	public W when;

	public Event(W w) {
		this.when = w;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "@" + when;
	}
}