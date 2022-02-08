package idawi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import idawi.net.LMI;
import idawi.service.DemoService;
import idawi.service.DeployerService;
import idawi.service.PingService;
import toools.io.Cout;

public class LucTests {

	public static void main(String[] args) {
		var a = new StringBuilder("toto");
		var b = new StringBuilder("toto");
		System.out.println(a.equals(b));
		System.out.println(a.hashCode());
	}
	
	public static void main2(String[] args) throws Throwable {
		Cout.debugSuperVisible("Starting test");

		// describes a component by its name only
		ComponentDescriptor me = new ComponentDescriptor();
		me.name = "c1";

		// trigger the creation of a component from its description
		Component c1 = new Component(me);

		// a shortcut for creating a component from a description
		Component c2 = new Component("c2");

		// connect those 2 components
		LMI.connect(c1, c2);

		// ask c1 to ping c2
		Service s = new Service(c1);
		Message pong = PingService.ping(s, c2.descriptor(), 1);
		System.out.println(pong);
		Component.stopPlatformThreads();
	}

	@Test
	public void twoComponentsConversation() throws CDLException {
		Cout.debugSuperVisible("Starting test");
		// describes a component by its name only
		ComponentDescriptor me = new ComponentDescriptor();
		me.name = "c1";

		// trigger the creation of a component from its description
		Component c1 = new Component(me);

		// a shortcut for creating a component from a description
		Component c2 = new Component("c2");

		// connect those 2 components
		LMI.connect(c1, c2);

		// ask c1 to ping c2
		Service s = new Service(c1);
		Message pong = PingService.ping(s, c2.descriptor(), 1);

		// be sure c1 got an answer
		assertNotEquals(null, pong);

		// clean
		Component.componentsInThisJVM.clear();
	}

	@Test
	public void operationSignatures() throws Throwable {
		Cout.debugSuperVisible("Starting test");
		Component c1 = new Component("c1");
		Component c2 = new Component("c2");
		LMI.connect(c1, c2);

		Service client = new Service(c1);
		assertEquals(5,
				(Integer) client.execf(new To(c2).o(DemoService.stringLength.class), 1, 1, "salut").get(0));
		assertEquals(53, (Integer) client.exec(new To(c2).o(DemoService.countFrom1toN.class), true, 100).returnQ
				.collect().resultMessages(100).get(53).content);
		assertEquals(7, (Integer) client.exec(new To(c2).o(DemoService.countFromAtoB.class), true,
				new DemoService.Range(0, 13)).returnQ.collect().resultMessages(13).get(7).content);

		Component.componentsInThisJVM.clear();
	}

	@Test
	public void waitingFirst() throws CDLException {
		Cout.debugSuperVisible("Starting test");
		Component root = new Component("root");
		Set<Component> others = root.lookup(DeployerService.class).deployInThisJVM(2, i -> "other-" + i, true, null);
		others.forEach(c -> LMI.connect(root, c));

		Service client = new Service(root);
		Set<ComponentDescriptor> ss = others.stream().map(c -> c.descriptor()).collect(Collectors.toSet());

		ComponentDescriptor first = client.exec(new To(ss).o(DemoService.waiting.class), true,
				new OperationParameterList(1)).returnQ.collectUntilFirstEOT().resultMessages(1).first().route
						.source().component;
		System.out.println(first);
//		assertEquals(7, (Double) );
		Component.componentsInThisJVM.clear();
	}

	@Test
	public void pingViaTCP() throws CDLException, IOException {
		Cout.debugSuperVisible("Starting test");

		// creates a component in this JVM
		Component master = new Component("master");
		master.descriptor().tcpPort = 56756;

		// and deploy another one in a separate JVM
		// they will communicate through standard streams
		ComponentDescriptor other = ComponentDescriptor.fromCDL("name=other_peer /  tcp_port=56757");
		master.lookup(DeployerService.class).deployOtherJVM(other, true, fdbck -> System.out.println(fdbck),
				p -> System.out.println("ok"));

		// asks the master to ping the other component
		Message pong = PingService.ping(new Service(master), other, 1);
		System.out.println("***** " + pong.route);

		// be sure it got an answer
		assertNotEquals(null, pong);

		// clean
		Component.componentsInThisJVM.clear();
	}

	@Test
	public void signature() {
		Cout.debugSuperVisible("Starting test");
		ComponentDescriptor me = new ComponentDescriptor();
		me.name = "c1";
		Component c1 = new Component(me);
		Component c2 = new Component("c2");
		LMI.connect(c1, c2);
		Service client = c1.lookup(DemoService.class);
		var len = client.execf(new To(c2).o(DemoService.stringLength.class), 1, 1, "hello");
		System.out.println(len);
		System.out.println(len);
		assertEquals(len, 5);

		// clean
		Component.componentsInThisJVM.clear();
	}

	@Test
	public void multihop() {
		Cout.debugSuperVisible("Starting test multihop");
		List<Component> l = new ArrayList<>();

		for (int i = 0; i < 10; ++i) {
			l.add(new Component());
		}

		LMI.chain(l);
		Message pong = PingService.ping(new Service(l.get(0)), l.get(l.size() - 1).descriptor(), 1);
		System.out.println(pong.route);
		assertNotEquals(pong, null);

		// clean
		Component.componentsInThisJVM.clear();
	}

}