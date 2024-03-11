package idawi.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import idawi.Component;
import idawi.EndpointParameterList;
import idawi.Idawi;
import idawi.Service;
import idawi.deploy.DeployerService;
import idawi.messaging.Message;
import idawi.routing.BlindBroadcasting;
import idawi.routing.ComponentMatcher;
import idawi.service.DemoService;
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
		Message pong = c1.bb().ping(c2).poll_sync();
		System.out.println(pong);
		Idawi.agenda.waitForCompletion();
	}

	@org.junit.Test
	public void twoComponentsConversation() throws Throwable {
		Idawi.agenda.start();

		Cout.debugSuperVisible("Starting test twoComponentsConversation");
		// trigger the creation of a component from its description
		Component c1 = new Component();

		// a shortcut for creating a component from a description
		Component c2 = new Component();

		// connect those 2 components
		Network.markLinkActive(c1, c2, SharedMemoryTransport.class, true, Set.of(c1, c2));

		// ask c1 to ping c2
		Message pong = c1.bb().ping(c2).poll_sync();

		// be sure c1 got an answer
		assertNotEquals(null, pong);

		// clean
		Component.componentsInThisJVM.clear();
		Idawi.agenda.waitForCompletion();
	}

	@Test
	public void manyMessages() throws Throwable {
		Idawi.agenda.start();

		Cout.debugSuperVisible("Starting test manyMessages");
		Component c1 = new Component();

		var target = c1.service(DeployerService.class, true).deployInNewJVM(msg -> System.out.println(msg));

		for (int i = 0; i < 100; ++i) {
			Message pong = c1.bb().ping(target).poll_sync();

			// be sure c1 got an answer
			assertNotEquals(null, pong);
		}

		Idawi.agenda.waitForCompletion();
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
		assertEquals(5,
				(int) c1.service(BlindBroadcasting.class, true).exec_rpc(c2, DemoService.stringLength.class, "salut"));
		Cout.debugSuperVisible(2);

		assertEquals(53, (int) c1.bb().exec(c2, DemoService.countFrom1toN.class, 100, true).returnQ.collector()
				.collectNResults(100).get(53));
		Cout.debugSuperVisible(3);

		assertEquals(7,
				(int) c1.bb().exec(c2, DemoService.countFromAtoB.class, new DemoService.Range(0, 13), true).returnQ
						.collector().collectNResults(13).get(7));
		Cout.debugSuperVisible(4);

		// assertEquals(7, c2.DemoService.countFromAtoB(0, 13).get(7).content);

		Idawi.agenda.waitForCompletion();

	}

	@Test
	public void waitingFirst() throws Throwable {
		Idawi.agenda.start();

		Cout.debugSuperVisible("Starting test waitingFirst");
		var root = new Component();
		List<Component> others = root.service(DeployerService.class, true).deployInThisJVM("c1", "c2");

		Set<Component> ss = new HashSet<>(others.stream().map(c -> c).toList());

		Component first = root.bb().exec(DemoService.class, DemoService.waiting.class, null,
				ComponentMatcher.multicast(ss), true, new EndpointParameterList(1), true).returnQ.collector()
				.collectWhile(c -> !c.messages.isEmpty()).messages.get(0).route.first().link.src.component;

		System.out.println(first);
//		assertEquals(7, (Double) );
		Idawi.agenda.waitForCompletion();

	}

	@Test
	public void pingViaTCP() throws Throwable {
		Cout.debugSuperVisible("Starting test pingViaTCP");
		Idawi.agenda.start();

		// creates a component in this JVM
		Component master = new Component();

		// and deploy another one in a separate JVM
		// they will communicate through standard streams
		var c = master.service(DeployerService.class).deployInNewJVM(fdbck -> System.out.println(fdbck));

		// asks the master to ping the other component
		Message pong = new Service(master).component.bb().ping(c).poll_sync();
		System.out.println("***** " + pong.route);

		// be sure it got an answer
		assertNotEquals(null, pong);

		Idawi.agenda.waitForCompletion();
	}

	@Test
	public void signature() throws Throwable {
		Idawi.agenda.start();

		Cout.debugSuperVisible("Starting signature test");
		Component c1 = new Component();
		Component c2 = new Component();
		new DemoService(c2);
		Network.markLinkActive(c1, c2, SharedMemoryTransport.class, true, Set.of(c1, c2));

		var rom = c1.bb().exec(c2, DemoService.stringLength.class, new EndpointParameterList("hello"), true);
		var c = rom.returnQ.collector();
		c.collect(5, 5, cc -> {
			Cout.debugSuperVisible(cc.messages.last());
			cc.stop = !cc.messages.isEmpty();
			Idawi.agenda.setTerminationCondition(() -> cc.stop);
		});

		var l = c.messages;
		int len = (int) l.resultMessages().contents().get(0);
		assertEquals(5, len);

		// clean
		Idawi.agenda.waitForCompletion();

	}

	@Test
	public void rest() throws Throwable {
		Idawi.agenda.start();

		Cout.debugSuperVisible("Starting REST test");
		Component c1 = new Component();
		var ws = c1.service(WebService.class);
		ws.startHTTPServer();
		NetUtilities.retrieveURLContent("http://localhost:" + ws.getPort() + "/api/" + c1);
		// clean
		Idawi.agenda.waitForCompletion();

	}

	@Test
	public void multihop() throws Throwable {
		Idawi.agenda.start();

		Cout.debugSuperVisible("Starting test multihop");
		List<Component> l = Component.createNComponent(10);

		Topologies.chain(l, (a, b) -> SharedMemoryTransport.class, l);
		var first = new Service(l.get(0));
		var last = l.get(l.size() - 1);
		Message pong = first.component.bb().ping(last).poll_sync();
		System.out.println(pong.route);
		assertNotEquals(pong, null);

		// clean
		Idawi.agenda.waitForCompletion();

	}

}