import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import idawi.CDLException;
import idawi.Component;
import idawi.ComponentInfo;
import idawi.MessageException;
import idawi.Service;
import idawi.service.ComponentDeployer;
import idawi.service.DummyService;
import idawi.service.ServiceManager;
import toools.io.Cout;

public class Test2 {

	public static void main(String[] args) throws CDLException, MessageException {
		new Test2().startService();
	}

	static class TestService extends Service {
		int i = 3;

		public TestService(Component t) {
			super(t);
			registerOperation("test", (ms, out) -> out.accept(i));
		}
	}

	@Test
	public void startService() throws CDLException, MessageException {
		Cout.debugSuperVisible("Starting test");
		Component c1 = new Component();
		ComponentInfo c2 = c1.lookupService(ComponentDeployer.class).deployLocalPeers(1, i -> "other-" + i, true, null)
				.iterator().next().descriptor();

		c1.lookupService(ServiceManager.class).start(TestService.class, c2, 10);

		Object r = c1.lookupService(DummyService.class).call(c2, TestService.class, "test").collect().throwAnyError()
				.resultMessages(1).first().content;
		assertEquals(r, 3);

		Component.componentsInThisJVM.clear();
		Component.stopPlatformThreads();
	}
}