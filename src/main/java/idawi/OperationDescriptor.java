package idawi;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Arrays;

public class OperationDescriptor implements Serializable {

	public final Class<?>[] parameterTypes;
	public final Class<?>[] returnTypes;
	private final String name;

	public OperationDescriptor(Method m) {
		this.parameterTypes = m.getParameterTypes();
		this.returnTypes = new Class[] { m.getReturnType() };
		this.name = m.getName();
	}


	@Override
	public String toString() {
		return Arrays.toString(returnTypes) + " " + name + "(" + Arrays.toString(parameterTypes) + ")";
	}
}
