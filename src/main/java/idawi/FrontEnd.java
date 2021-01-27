package idawi;

import java.util.Set;

public class FrontEnd {
	protected Service from;
	protected Set<ComponentDescriptor> target;

	public FrontEnd set(Service from, Set<ComponentDescriptor> to) {
		return this;
	}
}