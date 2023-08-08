package idawi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import idawi.messaging.MessageQueue;
import idawi.service.local_view.TypeOperationDescriptor;
import toools.io.Cout;
import toools.reflect.Clazz;

public abstract class TypedInnerClassEndpoint extends InnerClassEndpoint {
	private final Method method;

	// will be set by the service class
//	Service service;

	public TypedInnerClassEndpoint() {
		this.method = findMain();
	}


	@Override
	protected TypeOperationDescriptor createDescriptor() {
		return new TypeOperationDescriptor();
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
			throw new IllegalStateException(this + ": no public method found for that operation");
		} else if (methods.size() == 1) {
			return methods.get(0);
		} else {
			throw new IllegalStateException(getClass() + ": only one main method is allowed");
		}
	}

	@Override
	public final void impl(MessageQueue in)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		var exeMsg = in.poll_sync();
//		if (true)throw new Error("test error ");
		// Cout.debugSuperVisible("transaling");
		var parms = EndpointParameterList.from(exeMsg.content, method.getParameterTypes(), getClass());
		Cout.debugSuperVisible("calling");
		Object r = method.invoke(this, parms.toArray());

		if (method.getReturnType() != void.class) {
			service.component.bb().send(r, exeMsg.destination.replyTo);
//			service.component.bb().send(EOT.instance, input.replyTo);
		}
	}
}
