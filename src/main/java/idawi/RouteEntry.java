package idawi;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Objects;

public class RouteEntry implements Externalizable {
	public transient ComponentDescriptor component;
	public String componentID;
	public String protocolName;
	public double emissionDate;

	@Override
	public String toString() {
		return componentID + "-(" + protocolName + ")";
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof RouteEntry)) {
			return false;
		}

		RouteEntry e = (RouteEntry) o;
		return component.equals(e.component) && Objects.equals(protocolName, protocolName)
				&& emissionDate == e.emissionDate;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeUTF(component.name);
		out.writeUTF(protocolName);
		out.writeDouble(emissionDate);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		componentID = in.readUTF();
		protocolName = in.readUTF();
		emissionDate = in.readDouble();
	}
}
