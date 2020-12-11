package idawi.ui.gui;

import idawi.Component;
import idawi.ui.gui.UserFeedback.Entry.TYPE;

public class UserFeedback2console extends UserFeedback {

	public UserFeedback2console(Component peer) {
		super(peer);
	}

	@Override
	public String getFriendlyName() {
		return "shows feedback";
	}

	@Override
	protected void newFeedback(Entry e) {
		if (e.type == TYPE.error) {
			System.err.println("Error: " + e.value);
		}
		else if (e.type == TYPE.exception) {
			((Throwable) e.value).printStackTrace();
		}
		else if (e.type == TYPE.warning) {
			System.err.println("warning: " + e.value);
		}
		else if (e.type == TYPE.msg) {
			System.out.println(e.value);
		}
		else {
			throw new IllegalStateException();
		}
	}

}
