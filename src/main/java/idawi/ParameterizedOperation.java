package idawi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Consumer;

public class ParameterizedOperation<S extends Service> implements OperationStandardForm {
	private final Method method;
	protected S service;


	public ParameterizedOperation() {
		method = getClass().getDeclaredMethods()[0];
	}

	public Class<? extends Service> getService(){
		return service.getClass();
	}

	@Override
	public void accept(MessageQueue in)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Message msg = in.get_blocking();
		Consumer<Object> returns = r -> service.send(r, msg.replyTo, null);
		OperationParameterList parmsList = parmsList(msg);
		Object r = method.invoke(null, parmsList.toArray());

		if (method.getReturnType() != void.class) {
			returns.accept(r);
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
}