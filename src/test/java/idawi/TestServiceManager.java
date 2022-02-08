package idawi;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;

import org.junit.jupiter.api.Test;

import idawi.net.LMI;
import idawi.service.DemoService;
import idawi.service.ServiceManager;
import toools.io.Cout;

public class TestServiceManager {

	public static void main(String[] args) throws Throwable {
		new TestServiceManager().startStop();
	}

	public static Method search(Object value, String name, Object target) {
		for (var m : target.getClass().getMethods()) {
			if ((m.getModifiers() & Modifier.STATIC) == 1 && m.getName().startsWith("set") && m.getParameterCount() == 1) {
				if (value == null) {
					return m;
				} else if (m.getParameterTypes()[0].isAssignableFrom(value.getClass())) {
					return m;
				}
			}
		}

		return null;
	}

	@Test
	public void startStop() throws Throwable {
		MessageQueue.DEFAULT_TIMEOUT_IN_SECONDS = 1;
		Cout.debugSuperVisible("Starting test");
		Component a = new Component();
		Component b = new Component();
		LMI.connect(a, b);

		var bd = b.descriptor();

		var stub = new ServiceManager.Stub(new Service(a), new To(Set.of(bd)));

		stub.list().forEach(operationName -> System.out.println(operationName));

		if (!stub.has(DemoService.class)) {
			System.out.println("starting service " + DemoService.class);
			stub.start(DemoService.class);
		}

		stub.list().forEach(operationName -> System.out.println(operationName));
		System.out.println("stopping service " + DemoService.class);
		stub.stop(DemoService.class);
		stub.list().forEach(operationName -> System.out.println(operationName));
		System.out.println("starting service " + DemoService.class);
		stub.start(DemoService.class);
		stub.list().forEach(operationName -> System.out.println(operationName));
		// assertEquals(sms.list().contains(DummyService.class), false);

//		assertEquals(sms.list().contains(DummyService.class), true);

		Component.componentsInThisJVM.clear();
		Component.stopPlatformThreads();
	}
}