package idawi;

import java.lang.reflect.Method;

public abstract class OperationField extends Operation {
	// the name is set when the service scans for its fields operations
	String name;

	public String getName() {
		return name;
	}

	private Method findServerMethod() {
		Method r = null;

		for (var m : getClass().getDeclaredMethods()) {
			if (m.getName().equals("run")) {
				if (r != null) {
					throw new IllegalStateException("you cannot have multiple definitions of the run() methods");
				}

				r = m;
			}
		}

		return r;
	}
}
