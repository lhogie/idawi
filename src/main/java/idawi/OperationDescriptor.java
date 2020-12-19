package idawi;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class OperationDescriptor implements Serializable {

	public final Class<?>[] parameterTypes;
	public final List<String> parameterTypesNames;
	public final Class<?>[] returnTypes;
	public final String name;

	public OperationDescriptor(Method m) {
		this.parameterTypes = m.getParameterTypes();
		this.parameterTypesNames = Arrays.stream(m.getParameterTypes()).map(t -> t.getName())
				.collect(Collectors.toList());

		this.returnTypes = new Class[] { m.getReturnType() };
		this.name = m.getName();
	}

	@Override
	public String toString() {
		return Arrays.toString(returnTypes) + " " + name + "(" + Arrays.toString(parameterTypes) + ")";
	}
}
