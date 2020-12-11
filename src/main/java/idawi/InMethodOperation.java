package idawi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Consumer;

import toools.reflect.Clazz;

public class InMethodOperation extends AbstractOperation {
	public final Method method;
	private final Service service;
	public final OperationDescriptor signature;

	public InMethodOperation(Service s, Method m) {
		m.setAccessible(true);
		this.method = m;
		this.service = s;
		this.signature = new OperationDescriptor(m);
	}

	@Override
	public OperationDescriptor signature() {
		return signature;
	}
	@Override
	public String toString() {
		return signature.toString();
	}

	@Override
	protected void acceptImpl(Message msg, Consumer<Object> returns) throws Throwable {
		if (msg.content == null)
			msg.content = new OperationParameterList();
		
		if (msg.content instanceof OperationParameterList) {
			OperationParameterList parms = (OperationParameterList) msg.content;
			Class<?>[] signature = method.getParameterTypes();

			// use return keyword instead of returns consumer
			if (parms.size() == signature.length) {
				invoke(parms, returns);
			} else if (parms.size() + 1 == signature.length) {
				if (signature[signature.length - 1] == Consumer.class) {
					parms.add(returns);
					invoke(parms, returns);
				} else {
					throw new IllegalStateException("last parameter of operation " + method.getName()
							+ " should be of type " + Consumer.class.getName());
				}
			} else {
				throw new IllegalStateException("expecting parameters " + Arrays.toString(signature) + " for operation "
						+ method.getDeclaringClass().getName() + "." + method.getName() + " but received "
						+ Arrays.toString(Clazz.getClasses(parms.toArray())));
			}
		} else {
//			System.err.println(method.getName());
			Object r = method.invoke(service, msg, returns);

			if (method.getReturnType() != void.class) {
				returns.accept(r);
			}
		}
	}

	private void invoke(OperationParameterList parms, Consumer<Object> returns) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Object r = method.invoke(service, parms.toArray());

		if (method.getReturnType() != void.class) {
			returns.accept(r);
		}
	}
}
