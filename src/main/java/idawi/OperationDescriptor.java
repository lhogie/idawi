package idawi;

import java.lang.reflect.Method;
import java.util.Arrays;

public class OperationDescriptor {

	public Class<?>[] parameterTypes;
	public Class<?>[] returnTypes;
	private String name;

	public OperationDescriptor(Method m) {
		this.parameterTypes = m.getParameterTypes();
		this.returnTypes = new Class[] { m.getReturnType() };
		this.name = m.getName();
	}

	@Override
	public String toString() {
		return Arrays.toString(returnTypes) + " " + name + "(" + Arrays.toString(parameterTypes) + ")";
	}

	public OperationDescriptor(Class[] parameterTypes) {
		this.parameterTypes = parameterTypes;
		this.returnTypes = null;
	}
}
