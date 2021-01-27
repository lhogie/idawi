package idawi;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class InStaticInnerClassOperation extends Operation {
	private final Method backEndMethod;
	private final Service service;
	private final String description;
	private final String name;

	public InStaticInnerClassOperation(Service service, Class<? extends Operation2> innerClass) {
		super(null);
		this.service = service;
		this.name = innerClass.getName();

		var backends = backendMethods(innerClass);

		if (backends.size() == 1) {
			this.backEndMethod = backends.get(0);
		} else {
			throw new IllegalStateException("0 or > 1 backend found");
		}

		try {
			this.description = (String) innerClass.getField("description").get(null);
		} catch (SecurityException | IllegalArgumentException | IllegalAccessException | NoSuchFieldException e) {
			this.description = null;
		}
	}

	private List<Method> backendMethods(Class<? extends Operation2> operationInnerClass) {
		List<Method> r = new ArrayList<>();

		for (var i : operationInnerClass.getDeclaredClasses()) {
			if (Backend.class.isAssignableFrom(i)) {
				for (var m : i.getDeclaredMethods()) {
					r.add(m);
				}
			}
		}

		return r;
	}

	@Override
	public final String getName() {
		return name;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public void accept(MessageQueue input) throws Throwable {
		backEndMethod.invoke(service, input);
	}
}
