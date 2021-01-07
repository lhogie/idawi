package idawi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import toools.reflect.Clazz;

public class InMethodOperation extends Operation {
	private final Method method;
	private final Object target;

	public InMethodOperation(Object target, Method m) {
		m.setAccessible(true);
		this.method = m;
		this.target = target;

		if (isParameterized()) {
			this.descriptor.parameterTypes = Arrays.stream(m.getParameterTypes()).map(c -> c.getName())
					.collect(Collectors.toList());
		}
	}

	public boolean isParameterized() {
		var p = method.getParameterTypes();
		boolean standardForm = p.length == 2 && p[0] == Message.class && p[1] == Consumer.class;
		return !standardForm;
	}

	@Override
	public String toString() {
		return target.getClass().getName() + "." + getName() + "(" + method.getParameterTypes() + ")";
	}

	@Override
	public void accept(Message msg, Consumer<Object> returns)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		if (isParameterized()) {
			var types = method.getParameterTypes();
			OperationParameterList parmsList = parmsList(msg);

			// exact match, the message nor the "return" consumer are passed
			if (parmsList.size() == types.length) {
				invoke(returns, parmsList.toArray());
			} else if (types.length == parmsList.size() + 1) {
				// uses returns consumer
				if (types[types.length - 1] == Consumer.class) {
					parmsList.add(returns);
					invoke(returns, parmsList.toArray());
				} else {
					throw new IllegalStateException("only extra parameter allowed is " + Consumer.class.getName());
				}
			} else {
				throw new IllegalStateException("expecting parameters " + types + " for operation "
						+ method.getDeclaringClass().getName() + "." + method.getName() + " but received "
						+ Arrays.toString(Clazz.getClasses(parmsList.toArray())));
			}
		} else {
			invoke(returns, msg, returns);
		}
	}

	private OperationParameterList parmsList(Message msg) {
		if (msg.content instanceof OperationParameterList) {
			return (OperationParameterList) msg.content;
		} else if (msg.content instanceof OperationStringParameterList) {
			return OperationParameterList.from((OperationStringParameterList) msg.content, method.getParameterTypes());
		} else {
			var parmsList = new OperationParameterList();
			parmsList.add(msg.content);
			return parmsList;
		}
	}

	protected void invoke(Consumer<Object> returns, Object... parms)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Object r = method.invoke(target, parms);

		if (method.getReturnType() != void.class) {
			returns.accept(r);
		}
	}

	@Override
	public String getName() {
		return method.getName();
	}

	@Override
	public String getDescription() {
		return null;
	}

}
