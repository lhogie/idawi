package idawi.service;

import java.util.ArrayList;
import java.util.List;

import idawi.Component;
import idawi.IdawiExposed;
import idawi.Service;

public class ErrorLog extends Service {
	public final List<Throwable> errors = new ArrayList<>();

	public ErrorLog(Component peer) {
		super(peer);
		registerOperation(null, (msg, out) -> errors.add((Throwable) msg.content));
	}

	public void report(Throwable error) {
		errors.add(error);
		broadcast(error, 5);
	}

	public void report(String msg) {
		report(new Error(msg));
	}

	@IdawiExposed
	private List<Throwable> listErrors() {
		return errors;
	}
}
