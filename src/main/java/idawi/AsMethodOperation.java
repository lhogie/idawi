package idawi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

import toools.io.Cout;
import toools.text.TextUtilities;

public class AsMethodOperation extends Operation {
	public static class OperationID {
		public OperationID(Class<? extends Service> declaringService, String operationName) {
			this.declaringService = declaringService;
			this.operationName = operationName;
		}

		Class<? extends Service> declaringService;
		String operationName;
	}

	private final Method method;
	protected Service service;

	public AsMethodOperation(Method m, Service service) {
		this.method = Objects.requireNonNull(m);
		method.setAccessible(true);

		this.service = Objects.requireNonNull(service);

		if (isStandardForm() && method.getReturnType() != void.class) {
			throw new IllegalStateException(
					method.getDeclaringClass().getName() + "." + method.getName() + " should return void");
		}
	}

	public Class<? extends Service> getService() {
		return service.getClass();
	}

	@Override
	public void accept(MessageQueue in)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		if (isStandardForm()) {
			method.invoke(service, in);
		} else {
			Message parmMsg = in.get_blocking();
//			Cout.debug(parmMsg.content);
			OperationParameterList parms = OperationParameterList.from(this, parmMsg.content,
					method.getParameterTypes());
//			Cout.debug(parms);
			
			Object r = method.invoke(service, parms.toArray());

			if (method.getReturnType() != void.class) {
				service.send(r, parmMsg.requester);
			}
		}
	}
	




	public boolean isStandardForm() {
		var parms = method.getParameterTypes();
		return parms.length == 1 && parms[0] == MessageQueue.class;
	}

	public boolean isParameterized() {
		return !isStandardForm();
	}

	@Override
	protected Class<? extends Service> getDeclaringService() {
		return (Class<? extends Service>) method.getDeclaringClass();
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