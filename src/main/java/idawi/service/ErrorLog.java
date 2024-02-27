package idawi.service;

import java.util.ArrayList;
import java.util.List;

import idawi.Component;
import idawi.InnerClassEndpoint;
import idawi.Service;
import idawi.TypedInnerClassEndpoint;
import idawi.messaging.MessageQueue;
import idawi.routing.ComponentMatcher;
import toools.Exceptioons;

public class ErrorLog extends Service {
	public final List<Throwable> errors = new ArrayList<>();

	public ErrorLog(Component peer) {
		super(peer);
	}

	public class registerError extends InnerClassEndpoint {
		@Override
		public void impl(MessageQueue in) throws Throwable {
			errors.add((Throwable) in.poll_sync().content);
		}

		@Override
		public String getDescription() {
			return "register a new error";
		}
	}

	public class listError extends TypedInnerClassEndpoint {
		public List<Throwable> f() {
			return errors;
		}

		@Override
		public String getDescription() {
			return "gets the errors stored in this component";
		}
	}

	public void report(Throwable error) {
		error = Exceptioons.cause(error);
		error.printStackTrace();
		errors.add(error);
		component.bb().exec(getClass(), registerError.class, null, ComponentMatcher.all, false, error, true);
	}

	public void report(String msg) {
		report(new Error(msg));
	}

	public class list extends TypedInnerClassEndpoint {
		public List<Throwable> f() {
			return errors;
		}

		@Override
		public String getDescription() {
			return "retrieve all the errors know by this component";
		}
	}
}
