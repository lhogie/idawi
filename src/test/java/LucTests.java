import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.IOException;
import java.util.HashSet;

import org.junit.jupiter.api.Test;

import idawi.CDLException;
import idawi.Component;
import idawi.ComponentInfo;
import idawi.Message;
import idawi.MessageList;
import idawi.RouteEntry;
import idawi.Service;
import idawi.To;
import idawi.net.LMI;
import idawi.service.ComponentDeployer;
import idawi.service.DummyService;
import idawi.service.PingPong;
import toools.io.Cout;
import toools.io.ser.JavaSerializer;

public class LucTests {

	public static void main(String[] args) {
		new LucTests().signature();
	}

	@Test
	public void twoComponentsConversation() throws CDLException {
		Cout.debugSuperVisible("Starting test");
		// describes a component by its name only
		ComponentInfo me = new ComponentInfo();
		me.friendlyName = "c1";

		// trigger the creation of a component from its description
		Component c1 = new Component(me);

		// a shortcut for creating a component from a description
		Component c2 = new Component("name=c2");

		// connect those 2 components via shared memory
		LMI.connect(c1, c2);

		// ask c1 to ping c2
		Message pong = c1.lookupService(PingPong.class).ping(c2.descriptor(), 1);

		// be sure c1 got an answer
		assertNotEquals(null, pong);

		// clean
		Component.thingsInThisJVM.clear();

		// this prevents threads to hinder the termination of the program
		Component.stopPlatformThreads();
	}

	@Test
	public void pingViaSSH() throws CDLException, IOException {
		Cout.debugSuperVisible("Starting test");

		// creates a component in this JVM
		System.out.println("peeeeers: " + Component.thingsInThisJVM);
		Component c1 = new Component(ComponentInfo.fromPDL("name=c1"));

		// and deploy another one in a separate JVM
		// they will communicate through standard streams
		ComponentInfo c2 = ComponentInfo.fromPDL("name=c2");
		c1.lookupService(ComponentDeployer.class).deployOtherJVM(c2, true, fdbck -> System.out.println(fdbck),
				p -> System.out.println("ok"));

		// asks the master to ping the other component
		Message pong = c1.lookupService(PingPong.class).ping(c2, 5);
		System.out.println("pong: " + pong);

// be sure it got an answer
		assertNotEquals(null, pong);

		// clean
		Component.thingsInThisJVM.clear();

		// this prevents threads to hinder the termination of the program
		Component.stopPlatformThreads();
	}

	@Test
	public void pingViaTCP() throws CDLException, IOException {
		Cout.debugSuperVisible("Starting test");

		// creates a component in this JVM
		Component master = new Component("name=master /  tcp_port=56756");

		// and deploy another one in a separate JVM
		// they will communicate through standard streams
		ComponentInfo other = ComponentInfo.fromPDL("name=other_peer /  tcp_port=56757");
		master.lookupService(ComponentDeployer.class).deployOtherJVM(other, true, fdbck -> System.out.println(fdbck),
				p -> System.out.println("ok"));

		// asks the master to ping the other component
		Message pong = master.lookupService(PingPong.class).ping(other, 1);
		System.out.println("***** " + pong.route);

		// be sure it got an answer
		assertNotEquals(null, pong);

		// clean
		Component.thingsInThisJVM.clear();

		// this prevents threads to hinder the termination of the program
		Component.stopPlatformThreads();
	}

	@Test
	public void serializers() {
		Message a = new Message();
		a.to = new To();
		a.to.notYetReachedExplicitRecipients = new HashSet<>();
		a.to.notYetReachedExplicitRecipients.add(ComponentInfo.fromPDL("name=Luc"));
		a.to.service = DummyService.class;
		RouteEntry re = new RouteEntry();
		re.component = ComponentInfo.fromPDL("name=test");
		re.protocolName = "tcp";
		a.route.add(re);
		a.content = new Object[] { 4, true, new String[] { "1st element" } };
		Message clone = (Message) new JavaSerializer<>().clone(a);
		assertEquals(a, clone);
	}

	@Test
	public void signature() {
		Cout.debugSuperVisible("Starting test");
		ComponentInfo me = new ComponentInfo();
		me.friendlyName = "c1";
		Component c1 = new Component(me);
		Component c2 = new Component("name=c2");
		LMI.connect(c1, c2);
		Service client = c1.lookupService(DummyService.class);
		MessageList returns = client.call(c2, DummyService.class, "stringLength", "hello").collect();
		System.out.println(returns);
		int len = (Integer) returns.resultMessages(1).first().content;
		System.out.println(len);
		assertEquals(len, 5);

		// clean
		Component.thingsInThisJVM.clear();

		// this prevents threads to hinder the termination of the program
		Component.stopPlatformThreads();
	}

}