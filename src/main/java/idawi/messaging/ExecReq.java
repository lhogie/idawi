package idawi.messaging;

import java.io.Serializable;

import idawi.InnerClassEndpoint;
import idawi.routing.QueueAddress;
import toools.SizeOf;

public class ExecReq implements Serializable, SizeOf {
	public Class<? extends InnerClassEndpoint> endpointID;
	public int nbThreadsRequired = 1; // 0 makes it synchronous
	public QueueAddress replyTo;
	public long soonestExecTime = 0;
	public long latestExecTime = Long.MAX_VALUE;
	public boolean detachQueueAfterCompletion = true;
	public Object parms;

	@Override
	public long sizeOf() {
		return 10;
	}
	
	
	@Override
	public String toString() {
		return "exec: " + endpointID.getSimpleName() + "(" + parms + ")";
	}

}