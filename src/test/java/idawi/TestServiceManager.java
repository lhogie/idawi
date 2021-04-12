package idawi;

import java.util.Set;

import org.junit.jupiter.api.Test;

import idawi.net.LMI;
import idawi.service.DummyService;
import idawi.service.ServiceManager;
import toools.io.Cout;

public class TestServiceManager {

	public static void main(String[] args) throws Throwable {
		new TestServiceManager().startStop();
	}

	@Test
	public void startStop() throws Throwable {
		MessageQueue.DEFAULT_TIMEOUT_IN_SECONDS = 1;
		Cout.debugSuperVisible("Starting test");
		Component a = new Component();
		Component b = new Component();
		LMI.connect(a, b);

		var bd = b.descriptor();

		var stub = new ServiceManager.Stub(new Service(a), new ComponentAddress(Set.of(bd)));

		stub.list().forEach(operationName -> System.out.println(operationName));

		if (!stub.has(DummyService.class)) {
			System.out.println("starting service " + DummyService.class);
			stub.start(DummyService.class);
		}

		stub.list().forEach(operationName -> System.out.println(operationName));
		System.out.println("stopping service " + DummyService.class);
		stub.stop(DummyService.class);
		stub.list().forEach(operationName -> System.out.println(operationName));
		System.out.println("starting service " + DummyService.class);
		stub.start(DummyService.class);
		stub.list().forEach(operationName -> System.out.println(operationName));
		// assertEquals(sms.list().contains(DummyService.class), false);

//		assertEquals(sms.list().contains(DummyService.class), true);

		Component.componentsInThisJVM.clear();
		Component.stopPlatformThreads();
	}
}