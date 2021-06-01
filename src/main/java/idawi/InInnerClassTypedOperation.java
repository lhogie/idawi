package idawi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class InInnerClassTypedOperation extends InInnerClassOperation {

	private final Method method;
	Service service;

	public InInnerClassTypedOperation() {
		this.method = findMain();
	}

	private Method findMain() {
		List<Method> methods = new ArrayList<>(Arrays.asList());

		for (var m : getClass().getDeclaredMethods()) {
			if (!m.getName().contains("lambda$")) {
				methods.add(m);
			}
		}

		if (methods.isEmpty()) {
			throw new IllegalStateException("no main method defined");
		} else if (methods.size() == 1) {
			return methods.get(0);
		} else {
			List<Method> annotedMethods = new ArrayList<>(Arrays.asList());

			for (var m : methods) {
				if (m.isAnnotationPresent(IdawiMain.class)) {
					annotedMethods.add(m);
				}
			}

			if (methods.size() == 1) {
				return methods.get(0);
			} else {
				throw new IllegalStateException("only one main method is allowed");
			}
		}
	}

	@Override
	public final void accept(MessageQueue in)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Message msg = in.get_blocking();
		OperationParameterList parmsList = OperationParameterList.from(this, msg.content, method.getParameterTypes());
		Object r = method.invoke(null, parmsList.toArray());

		if (method.getReturnType() != void.class) {
			service.send(r, msg.requester);
		}
	}

}
