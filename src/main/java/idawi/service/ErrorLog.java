package idawi.service;

import java.util.ArrayList;
import java.util.List;

import idawi.AsMethodOperation.OperationID;
import idawi.Component;
import idawi.IdawiOperation;
import idawi.InInnerClassTypedOperation;
import idawi.MessageQueue;
import idawi.Service;
import idawi.ServiceAddress;
import idawi.Utils;

public class ErrorLog extends Service {
	public final List<Throwable> errors = new ArrayList<>();

	public ErrorLog(Component peer) {
		super(peer);
	}

	public static OperationID registerError;

	@IdawiOperation
	public void registerError(MessageQueue in) {
		errors.add((Throwable) in.get_blocking().content);
	}

	public void report(Throwable error) {
		error = Utils.cause(error);
		error.printStackTrace();
		errors.add(error);
		exec(new ServiceAddress(), registerError, false, error);
	}

	public void report(String msg) {
		report(new Error(msg));
	}

	@IdawiOperation
	public class list extends InInnerClassTypedOperation {
		public List<Throwable> f() {
			return errors;
		}
	}
}
