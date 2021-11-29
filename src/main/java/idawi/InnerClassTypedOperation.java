package idawi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import toools.reflect.Clazz;

public abstract class InnerClassTypedOperation extends InnerClassOperation {

	private final Method method;
	Service service;

	public InnerClassTypedOperation() {
		this.method = findMain();
	}

	private Method findMain() {
		List<Method> methods = new ArrayList<>(Arrays.asList());

		for (var m : getClass().getDeclaredMethods()) {
			if (!m.getName().contains("lambda$") && (m.getModifiers() & Modifier.PUBLIC) != 0
					&& !Clazz.hasMethod(getClass().getSuperclass(), m.getName(), m.getParameterTypes())) {
				methods.add(m);
			}
		}

		if (methods.isEmpty()) {
			throw new IllegalStateException("no main method defined");
		} else if (methods.size() == 1) {
			return methods.get(0);
		} else {
			throw new IllegalStateException(getClass() + ": only one main method is allowed");
		}
	}

	@Override
	public final void exec(MessageQueue in)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Message msg = in.get_blocking();
		OperationParameterList parmsList = OperationParameterList.from(this, msg.content, method.getParameterTypes());
		Object r = method.invoke(this, parmsList.toArray());

		if (method.getReturnType() != void.class) {
			service.send(r, msg.replyTo);
		}
	}

}
