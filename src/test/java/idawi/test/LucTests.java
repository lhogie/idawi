package idawi.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.List;
import java.util.Set;

import org.junit.Test;

import idawi.Component;
import idawi.Idawi;
import idawi.deploy.DeployerService;
import idawi.messaging.Message;
import idawi.routing.BlindBroadcasting;
import idawi.service.DemoService;
import idawi.service.DemoService.countFrom1toN.AAA;
import idawi.service.DemoService.countFromAtoB;
import idawi.service.DemoService.countFromAtoB.Range;
import idawi.service.DemoService.stringLength;
import idawi.service.local_view.Network;
import idawi.service.web.WebService;
import idawi.transport.SharedMemoryTransport;
import idawi.transport.Topologies;
import toools.io.Cout;
import toools.net.NetUtilities;

public class LucTests {

	public static void main(String[] args) throws Throwable {
		new LucTests().operationSignatures();
	}

	public static void main2(String[] args) throws Throwable {
		Cout.debugSuperVisible("Starting test main2");

		// trigger the creation of a component from its description
		Component c1 = new Component();

		// a shortcut for creating a component from a description
		Component c2 = new Component();

		// connect those 2 components
		Network.markLinkActive(c1, c2, SharedMemoryTransport.class, true, Set.of(c1, c2));

		// ask c1 to ping c2
		Idawi.agenda.start();
		Message pong = c1.defaultRoutingProtocol().ping(c2);
		System.out.println(pong);
	}

	@Test
	public void manyMessages() throws Throwable {
		Idawi.agenda.start();

		Cout.debugSuperVisible("Starting test manyMessages");
		Component c1 = new Component();

		var target = c1.service(DeployerService.class, true).newLocalJVM("test");

		for (int i = 0; i < 100; ++i) {
			Message pong = c1.defaultRoutingProtocol().ping(target);

			// be sure c1 got an answer
			assertNotEquals(null, pong);
		}

	}

	@Test
	public void operationSignatures() throws Throwable {
		Idawi.agenda.start();

		Cout.debugSuperVisible("Starting test operationSignatures");
		Component c1 = new Component();
		Component c2 = new Component();
		c2.service(DemoService.class, true);
		Network.markLinkActive(c1, c2, SharedMemoryTransport.class, true, Set.of(c1, c2));

		Cout.debugSuperVisible(c1.outLinks());

//		RoutingListener.debug_on(c1, c2);
		assertEquals(5, (int) c1.defaultRoutingProtocol().exec_rpc(c2, DemoService.class,
				stringLength.class, msg -> msg.content = "salut"));
		Cout.debugSuperVisible(2);

		assertEquals(53, (int) c1.bb().exec(c2, DemoService.class, DemoService.countFrom1toN.class, msg -> {
			msg.content = new AAA();
			msg.content.n = 100;
			msg.content.sleepTime = 1;
		}).returnQ.collector().collectNResults(1, 100).get(53));
		Cout.debugSuperVisible(3);

		assertEquals(7, (int) c1.bb().exec(c2, DemoService.class, countFromAtoB.class,
				msg -> msg.content = new Range(0, 13)).returnQ.collector().collectNResults(1, 13).get(7));
		Cout.debugSuperVisible(4);

		// assertEquals(7, c2.DemoService.countFromAtoB(0, 13).get(7).content);
	}

	@Test
	public void deployAndPing() throws Throwable {
		Cout.debugSuperVisible("Starting test pingViaTCP");
		Idawi.agenda.start();

		// creates a component in this JVM
		Component master = new Component();

		// and deploy another one in a separate JVM
		// they will communicate through standard streams
		var c = master.need(DeployerService.class).newLocalJVM("test");

		// asks the master to ping the other component
		Message pong = master.defaultRoutingProtocol().ping(c);
		System.out.println("***** " + pong.route);

		// be sure it got an answer
		assertNotEquals(null, pong);

	}

	@Test
	public void signature() throws Throwable {
		Idawi.agenda.start();

		Cout.debugSuperVisible("Starting signature test");
		Component c1 = new Component();
		Component c2 = new Component();
		new DemoService(c2);
		Network.markLinkActive(c1, c2, SharedMemoryTransport.class, true, Set.of(c1, c2));

		var rom = c1.bb().exec(c2, DemoService.class, stringLength.class, msg -> msg.content = "hello");
		var c = rom.returnQ.collector();
		c.collect(5, cc -> {
			Cout.debugSuperVisible(cc.messages.last());
			cc.gotEnough = !cc.messages.isEmpty();
			Idawi.agenda.setTerminationCondition(() -> cc.gotEnough);
		});

		var l = c.messages;
		int len = (int) l.resultMessages().contents().get(0);
		assertEquals(5, len);

		// clean

	}

	@Test
	public void rest() throws Throwable {
		Idawi.agenda.start();

		Cout.debugSuperVisible("Starting REST test");
		Component c1 = new Component();
		var ws = c1.need(WebService.class);
		ws.startHTTPServer();
		var out = NetUtilities.retrieveURLContent("http://localhost:" + ws.DEFAULT_PORT + "/api/" + c1);
		System.out.println(new String(out));
		// clean

	}

	@Test
	public void multihop() throws Throwable {
		Idawi.agenda.start();

		Cout.debugSuperVisible("Starting test multihop");
		List<Component> l = Component.createNComponent(10);

		Topologies.chain(l, (a, b) -> SharedMemoryTransport.class, l);
		Cout.debugSuperVisible(l.getFirst().localView().g);
		Message pong = l.getFirst().defaultRoutingProtocol().ping(l.getLast());
		System.out.println(pong.route);
		assertNotEquals(pong, null);

	}

}