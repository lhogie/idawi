package idawi;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Objects;

import idawi.AsMethodOperation.OperationID;

public class OperationAddress implements Externalizable {
	private static final long serialVersionUID = 1L;
	public ServiceAddress service;
	public String opid;

	public OperationAddress() {

	}

	public OperationAddress(ServiceAddress sa, String opid) {
		this.service = sa;
		this.opid = opid;
	}

	public OperationAddress(ComponentAddress sa, OperationID opid) {
		this.service = new ServiceAddress(sa, opid.declaringService);
		this.opid = opid.operationName;
	}


	public OperationAddress o(ServiceAddress s) {
		return new OperationAddress(s, opid);
	}

	@Override
	public String toString() {
		return service.toString() + "->" + opid;
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof OperationAddress))
			return false;

		OperationAddress t = (OperationAddress) o;
		return super.equals(o) && Objects.equals(service, t.service);
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(service);
		out.writeObject(opid);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		service = (ServiceAddress) in.readObject();
		opid = (String) in.readObject();
	}
}
