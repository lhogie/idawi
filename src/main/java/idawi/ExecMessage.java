package idawi;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class ExecMessage extends Message {
	public String operationName;
	public boolean premptive = false;

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		out.writeUTF(operationName);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		this.operationName = in.readUTF();
	}
}
