package idawi.service;

import java.util.ArrayList;
import java.util.List;

import idawi.Component;
import idawi.InnerClassOperation;
import idawi.Service;
import idawi.TypedInnerClassOperation;
import idawi.Utils;
import idawi.messaging.MessageQueue;
import idawi.routing.ComponentMatcher;

public class ErrorLog extends Service {
	public final List<Throwable> errors = new ArrayList<>();

	public ErrorLog(Component peer) {
		super(peer);
		registerOperation(registerError = new registerError());
		operations.add(new registerError());
	}

	public static registerError registerError;

	public class registerError extends InnerClassOperation {
		@Override
		public void impl(MessageQueue in) throws Throwable {
			errors.add((Throwable) in.poll_sync().content);
		}

		@Override
		public String getDescription() {
			return "register a new error";
		}
	}

	public class listError extends TypedInnerClassOperation {
		public List<Throwable> f() {
			return errors;
		}

		@Override
		public String getDescription() {
			return "gets the errors stored in this component";
		}
	}

	public void report(Throwable error) {
		error = Utils.cause(error);
		error.printStackTrace();
		errors.add(error);
		component.	bb().exec(registerError.class, null, ComponentMatcher.all, false, error);
	}

	public void report(String msg) {
		report(new Error(msg));
	}

	public class list extends TypedInnerClassOperation {
		public List<Throwable> f() {
			return errors;
		}

		@Override
		public String getDescription() {
			return "retrieve all the errors know by this component";
		}
	}
}
