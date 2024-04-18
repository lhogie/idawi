package idawi.service;

import java.util.ArrayList;
import java.util.List;

import idawi.Component;
import idawi.ProcedureEndpoint;
import idawi.Service;
import idawi.SupplierEndPoint;
import idawi.routing.ComponentMatcher;
import toools.Exceptioons;

public class ErrorLog extends Service {
	public final List<Throwable> errors = new ArrayList<>();

	public ErrorLog(Component peer) {
		super(peer);
	}

	public class registerError extends ProcedureEndpoint<Throwable> {
		@Override
		public void doIt(Throwable err) throws Throwable {
			errors.add(err);
		}

		@Override
		public String getDescription() {
			return "register a new error";
		}
	}

	public class listError extends SupplierEndPoint<List<Throwable>> {
		@Override
		public List<Throwable> get() {
			return errors;
		}

		@Override
		public String r() {
			return "the errors stored in this component";
		}
	}

	public void report(Throwable error) {
		error = Exceptioons.cause(error);
		error.printStackTrace();
		errors.add(error);
		final var err = error;
		exec(ComponentMatcher.all, getClass(), registerError.class, msg -> msg.content = err);
	}

	public void report(String msg) {
		report(new Error(msg));
	}

}
