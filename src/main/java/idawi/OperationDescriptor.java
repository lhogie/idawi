package idawi;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class OperationDescriptor implements Serializable {

	public final List<String> parameterTypes;
	public final List<String> parameterTypesNames;
	public final String[] returnTypes;
	public final String name;

	public OperationDescriptor(Method m) {
		this.parameterTypes = Arrays.stream(m.getParameterTypes()).map(c -> c.getName()).collect(Collectors.toList());
		this.parameterTypesNames = Arrays.stream(m.getParameterTypes()).map(t -> t.getName())
				.collect(Collectors.toList());

		this.returnTypes = new String[] { m.getReturnType().getName() };
		this.name = m.getName();
	}

	@Override
	public String toString() {
		return Arrays.toString(returnTypes) + " " + name + "(" + parameterTypes + ")";
	}
}
