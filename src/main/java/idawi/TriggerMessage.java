package idawi;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class TriggerMessage extends Message {
	public String operationName;
	public boolean premptive = false;

	// just an idea
	public int repeat = 1;

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

	@Override
	public String toString() {
		return "trigger " + super.toString();
	}
}
