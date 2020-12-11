package idawi.ui.gui;

import java.util.Vector;

import idawi.Component;
import idawi.ComponentInfo;
import idawi.Service;
import idawi.ui.gui.UserFeedback.Entry.TYPE;

public abstract class UserFeedback extends Service {
	private Vector<Entry> history = new Vector<>();

	public static class Entry {
		public enum TYPE {
			msg, warning, exception, error
		}

		Entry(ComponentInfo source, TYPE t, Object v) {
			this.type = t;
			this.value = v;
			this.source = source;
		}

		final ComponentInfo source;
		final TYPE type;
		final Object value;
	}

	public UserFeedback(Component peer) {
		super(peer);

		for (TYPE type : TYPE.values()) {
			registerOperation(type.name(), (msg, returns) -> {
				Entry e = new Entry(msg.route.source().component, type, msg.content);
				history.add(e);
				newFeedback(e);
			});
		}
	}

	protected abstract void newFeedback(Entry e);

	public void report(TYPE type, Object value) {
		Entry e = new Entry(component.descriptor(), type, value);
		history.add(e);
		newFeedback(e);
	}

}
