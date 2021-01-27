package idawi.service;

import java.io.Serializable;

import idawi.EOT;
import idawi.MessageQueue;
import idawi.Service;
import idawi.To;
import toools.util.Date;

public class OperationStub implements Serializable {
	private final To toInputQueue;
	private transient final Service from;
	private transient final MessageQueue returnQueue;

	public static class InitialContent implements Serializable {
		public String operationName;
		public Object[] input;
	}

	public OperationStub(Service from, To toOperation, Object... input) {
		this.from = from;
		long ID = Date.timeNs();
		InitialContent c = new InitialContent();
		c.operationName = toOperation.operationOrQueue;
		c.input = input;
		this.toInputQueue = new To(toOperation.notYetReachedExplicitRecipients, toOperation.service, toOperation.operationOrQueue + "-" + ID);
		this.returnQueue = from.send(c, toInputQueue);
	}

	public void send(Object... o) {
		from.send(o, toInputQueue);
	}

	public void dispose() {
		send(EOT.instance);
	}

}
