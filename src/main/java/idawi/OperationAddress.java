package idawi;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Objects;

public class OperationAddress implements Externalizable {
	private static final long serialVersionUID = 1L;
	public ServiceAddress sa;
	public String opid;

	public OperationAddress() {

	}

	public OperationAddress(ServiceAddress sa, String opid) {
		this.sa = sa;
		this.opid = opid;
	}

	public OperationAddress(ComponentAddress ca, Class<? extends InnerClassOperation> opid) {
		this(new ServiceAddress(ca, (Class<? extends Service>) opid.getEnclosingClass()),
				InnerClassOperation.name(opid));
	}

	public OperationAddress o(ServiceAddress s) {
		return new OperationAddress(s, opid);
	}

	@Override
	public String toString() {
		return sa.toString() + "->" + opid;
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
		return super.equals(o) && Objects.equals(sa, t.sa);
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(sa);
		out.writeObject(opid);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		sa = (ServiceAddress) in.readObject();
		opid = (String) in.readObject();
	}
}
