package idawi;

import java.util.ArrayList;
import java.util.Collections;

public class Route extends ArrayList<RouteEntry> {
	private static final long serialVersionUID = 1L;

	public RouteEntry source() {
		return isEmpty() ? null : get(0);
	}

	public RouteEntry last() {
		return isEmpty() ? null : get(size() - 1);
	}

	public Route reverse() {
		Route r = new Route();
		r.addAll(this);
		Collections.reverse(r);
		return r;
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append('[');
		int len = size();

		for (int i = 0; i < len; ++i) {
			b.append(get(i));

			if (i < len - 1) {
				b.append(", ");
			}
		}

		b.append(']');
		return b.toString();
	}

	public void add(ComponentDescriptor d) {
		RouteEntry e = new RouteEntry();
		e.component = d;
		add(d);
	}
	
	@Override
	public boolean equals(Object o) {
		return o instanceof Route && super.equals(o);
	}

}
