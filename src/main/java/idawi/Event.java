package idawi;

import java.io.Serializable;

public abstract class Event<W extends When> implements Runnable, Serializable {
	public W when;
	String name;

	public Event(W w) {
		this(null, w);
	}

	public Event(String name, W w) {
		this.when = w;
		this.name = name;
	}

	@Override
	public final String toString() {
		String s = name == null ? getClass().getSimpleName() : name;
		return s + "@" + when;
	}
}