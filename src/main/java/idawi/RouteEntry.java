package idawi;

import java.io.Serializable;
import java.util.Objects;

public class RouteEntry implements Serializable {
	public ComponentInfo component;
	public String protocolName;
	public double emissionDate;

	@Override
	public String toString() {
		return component.friendlyName + "/" + protocolName;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof RouteEntry)) {
			return false;
		}

		RouteEntry e = (RouteEntry) o;
		return component.equals(e.component) && Objects.equals(protocolName, protocolName) && emissionDate == e.emissionDate;
	}
}
