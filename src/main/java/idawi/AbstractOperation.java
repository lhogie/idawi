package idawi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Consumer;

import toools.reflect.Clazz;

public class AbstractOperation {
	int nbCalls;
	double totalDuration;
	private final Method method;
	private final Object target;
	public final OperationDescriptor descriptor;

	public AbstractOperation(Object s, Method m) {
		m.setAccessible(true);
		this.method = m;
		this.target = s;
		this.descriptor = new OperationDescriptor(m);
	}

	@Override
	public String toString() {
		return descriptor().toString();
	}

	public double avgDuration() {
		return totalDuration / nbCalls;
	}

	public int nbCalls() {
		return nbCalls;
	}

	public OperationDescriptor descriptor() {
		return descriptor;
	}

	public void accept(Message msg, Consumer<Object> returns)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Class<?>[] types = method.getParameterTypes();

		if (types.length == 0) {
			invoke(returns);
		} else if (types.length == 1 && types[0] == Message.class) {
			invoke(returns, msg);
		} else if (types.length == 1 && types[0] == Consumer.class) {
			invoke(returns, returns);
		} else if (types.length == 2 && types[0] == Message.class && types[1] == Consumer.class) {
			invoke(returns, msg, returns);
		} else {
			// we have a parameterized operation
			OperationParameterList parms;

			if (msg.content instanceof OperationParameterList) {
				parms = (OperationParameterList) msg.content;
			} else if (msg.content instanceof OperationStringParameterList) {
				parms = OperationParameterList.from((OperationStringParameterList) msg.content,
						method.getParameterTypes());
			} else {
				throw new IllegalArgumentException("operation " + descriptor.name
						+ " is parameterized. As such, it expects message content is a parameter list, but it founds a "
						+ msg.content.getClass().getName());
			}

			// exact match, the message nor the "return" consumer are passed
			if (parms.size() == descriptor.parameterTypes.length) {
				invoke(returns, parms.toArray());
			} else if (descriptor.parameterTypes.length == parms.size() + 1) {
				// uses returns consumer
				if (descriptor.parameterTypes[descriptor.parameterTypes.length - 1] == Consumer.class) {
					parms.add(returns);
					invoke(returns, parms.toArray());
				} else {
					throw new IllegalStateException("operation '" + descriptor.name + "': received parms: " + parms);
				}
			} else {
				throw new IllegalStateException("expecting parameters " + Arrays.toString(descriptor.parameterTypes)
						+ " for operation " + method.getDeclaringClass().getName() + "." + method.getName()
						+ " but received " + Arrays.toString(Clazz.getClasses(parms.toArray())));
			}
		}
	}

	private void invoke(Consumer<Object> returns, Object... parms)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Object r = method.invoke(target, parms);

		if (method.getReturnType() != void.class) {
			returns.accept(r);
		}
	}
}
