package idawi;

import java.io.Serializable;

public class ReturnInfo implements Serializable {

	
	private static final long serialVersionUID = 1L;
	public Object serviceID;
	public Object queueID;

	@Override
	public String toString() {
		return serviceID + "/" + queueID;
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		ReturnInfo a = (ReturnInfo) obj;
		return a.serviceID.equals(serviceID) && a.queueID.equals(queueID);
	}
}