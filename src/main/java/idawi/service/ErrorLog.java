package idawi.service;

import java.util.ArrayList;
import java.util.List;

import idawi.Component;
import idawi.InnerOperation;
import idawi.TypedInnerOperation;
import idawi.MessageQueue;
import idawi.Service;
import idawi.Utils;

public class ErrorLog extends Service {
	public final List<Throwable> errors = new ArrayList<>();

	public ErrorLog(Component peer) {
		super(peer);
		registerOperation(registerError = new registerError());
		operations.add(new registerError());
	}

	public static registerError registerError;

	public class registerError extends InnerOperation {
		@Override
		public void impl(MessageQueue in) throws Throwable {
			errors.add((Throwable) in.poll_sync().content);
		}

		@Override
		public String getDescription() {
			// TODO Auto-generated method stub
			return null;
		}
	}

	public class listError extends TypedInnerOperation {
		public List<Throwable> f() {
			return errors;
		}

		@Override
		public String getDescription() {
			// TODO Auto-generated method stub
			return null;
		}
	}

	public void report(Throwable error) {
		error = Utils.cause(error);
		error.printStackTrace();
		errors.add(error);
		exec(ca().o(registerError.class), false, error);
	}

	public void report(String msg) {
		report(new Error(msg));
	}

	public class list extends TypedInnerOperation {
		public List<Throwable> f() {
			return errors;
		}

		@Override
		public String getDescription() {
			return "retrieve all the errors know by this component";
		}
	}
}
