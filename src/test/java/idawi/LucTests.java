package idawi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import idawi.net.LMI;
import idawi.service.ComponentDeployer;
import idawi.service.DummyService;
import idawi.service.PingService;
import toools.io.Cout;
import toools.io.ser.JavaSerializer;

public class LucTests {

	public static void main(String[] args) throws Throwable {

		Consumer<Message> c = m -> {
		};
		
		System.out.println(c.getClass());
		
	
		
		System.out.println("gen " + c.getClass().getTypeName());
		System.out.println(((((ParameterizedType) c.getClass().getInterfaces()[0].getGenericSuperclass())
				.getActualTypeArguments())));

		System.out.println(c.getClass().getGenericSuperclass());
		System.out.println(c.getClass().getGenericSuperclass() instanceof ParameterizedType);

		for (var a : ((ParameterizedType) c.getClass().getGenericSuperclass()).getActualTypeArguments()) {
			System.out.println("< " + a);
		}
		Component.stopPlatformThreads();
	}

	@Test
	public void twoComponentsConversation() throws CDLException {
		Cout.debugSuperVisible("Starting test");
		// describes a component by its name only
		ComponentDescriptor me = new ComponentDescriptor();
		me.friendlyName = "c1";

		// trigger the creation of a component from its description
		Component c1 = new Component(me);

		// a shortcut for creating a component from a description
		Component c2 = new Component("name=c2");

		// connect those 2 components
		LMI.connect(c1, c2);

		// ask c1 to ping c2
		Message pong = c1.lookupService(PingService.class).ping(c2.descriptor(), 1);

		// be sure c1 got an answer
		assertNotEquals(null, pong);

		// clean
		Component.componentsInThisJVM.clear();
	}

	@Test
	public void operationSignatures() throws Throwable {
		Cout.debugSuperVisible("Starting test");
		Component c1 = new Component("name=c1");
		Component c2 = new Component("name=c2");
		LMI.connect(c1, c2);

		Service client = new Service(c1);
		assertEquals(5,
				(Integer) client.call(new To(c2.descriptor(), DummyService.class, "stringLength"), "salut").get());
		assertEquals(53, (Integer) client.send(100, new To(c2.descriptor(), DummyService.class, "countFrom1toN"))
				.collect().resultMessages(100).get(53).content);
		assertEquals(7, (Integer) client.call(new To(c2.descriptor(), DummyService.class, "countFromAtoB"), 0, 13)
				.collect().resultMessages(13).get(7).content);

		Component.componentsInThisJVM.clear();
	}

	@Test
	public void waitingFirst() throws CDLException {
		Cout.debugSuperVisible("Starting test");
		Component root = new Component("name=root");
		Set<Component> others = root.lookupService(ComponentDeployer.class).deployLocalPeers(2, i -> "other-" + i, true,
				null);
		others.forEach(c -> LMI.connect(root, c));

		Service client = new Service(root);
		Set<ComponentDescriptor> ss = others.stream().map(c -> c.descriptor()).collect(Collectors.toSet());

		ComponentDescriptor first = client.call(new To(ss, DummyService.class, "waiting"), 1).collectUntilFirstEOT()
				.resultMessages(1).first().route.source().component;
		System.out.println(first);
//		assertEquals(7, (Double) );
		Component.componentsInThisJVM.clear();
	}

	@Test
	public void pingViaTCP() throws CDLException, IOException {
		Cout.debugSuperVisible("Starting test");

		// creates a component in this JVM
		Component master = new Component("name=master /  tcp_port=56756");

		// and deploy another one in a separate JVM
		// they will communicate through standard streams
		ComponentDescriptor other = ComponentDescriptor.fromCDL("name=other_peer /  tcp_port=56757");
		master.lookupService(ComponentDeployer.class).deployOtherJVM(other, true, fdbck -> System.out.println(fdbck),
				p -> System.out.println("ok"));

		// asks the master to ping the other component
		Message pong = master.lookupService(PingService.class).ping(other, 1);
		System.out.println("***** " + pong.route);

		// be sure it got an answer
		assertNotEquals(null, pong);

		// clean
		Component.componentsInThisJVM.clear();
	}

	@Test
	public void serializers() {
		Message a = new Message();
		a.to = new To();
		a.to.notYetReachedExplicitRecipients = new HashSet<>();
		a.to.notYetReachedExplicitRecipients.add(ComponentDescriptor.fromCDL("name=Luc"));
		a.to.service = DummyService.class;
		RouteEntry re = new RouteEntry();
		re.component = ComponentDescriptor.fromCDL("name=test");
		re.protocolName = "tcp";
		a.route.add(re);
		a.content = new Object[] { 4, true, new String[] { "1st element" } };
		Message clone = (Message) new JavaSerializer<>().clone(a);
		System.out.println("orig:  " + a);
		System.out.println("clone: " + clone);
		assertEquals(a, clone);
	}

	@Test
	public void signature() {
		Cout.debugSuperVisible("Starting test");
		ComponentDescriptor me = new ComponentDescriptor();
		me.friendlyName = "c1";
		Component c1 = new Component(me);
		Component c2 = new Component("name=c2");
		LMI.connect(c1, c2);
		Service client = c1.lookupService(DummyService.class);
		MessageList returns = client.call(new To(c2, DummyService.class, "stringLength"), "hello").collect();
		System.out.println(returns);
		int len = (Integer) returns.resultMessages(1).first().content;
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
		Message pong = l.get(0).lookupService(PingService.class).ping(l.get(l.size() - 1).descriptor(), 1);
		System.out.println(pong.route);
		assertNotEquals(pong, null);

		// clean
		Component.componentsInThisJVM.clear();
	}

}