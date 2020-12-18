import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import idawi.CDLException;
import idawi.Component;
import idawi.ComponentInfo;
import idawi.MessageException;
import idawi.MessageQueue;
import idawi.Service;
import idawi.net.LMI;
import idawi.service.ComponentDeployer;
import idawi.service.DummyService;
import idawi.service.PingPong;
import idawi.service.ServiceManager;
import toools.io.Cout;

public class TestServiceManager {

	public static void main(String[] args) throws CDLException, MessageException {
		new TestServiceManager().startStop();
	}


	@Test
	public void startStop() throws CDLException, MessageException {
		MessageQueue.DEFAULT_TIMEOUT_IN_SECONDS = 1;
		Cout.debugSuperVisible("Starting test");
		Component a = new Component();
		Component b = new Component();
		LMI.connect(a, b);

		var bd = b.descriptor();
		
		System.out.println(a.lookupService(PingPong.class).ping(bd, 1));
		
		var sms = new ServiceManager.Stub(a, bd);
		System.out.println(sms.list());
		sms.stop(DummyService.class);
//		assertEquals(sms.list().contains(DummyService.class), false);
		System.out.println(sms.list());
		sms.start(DummyService.class);
		System.out.println(sms.list());

//		assertEquals(sms.list().contains(DummyService.class), true);

		Component.componentsInThisJVM.clear();
		Component.stopPlatformThreads();
	}
}