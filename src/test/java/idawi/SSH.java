package idawi;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.IOException;
import java.util.Set;

import org.junit.jupiter.api.Test;

import idawi.CDLException;
import idawi.Component;
import idawi.ComponentInfo;
import idawi.Message;
import idawi.service.ComponentDeployer;
import idawi.service.PingPong;
import toools.io.Cout;

public class SSH {
	public static void main(String[] args) throws Exception {
		new SSH().pingViaSSH();
	}

	public static final String ssh = "musclotte.inria.fr";

	@Test
	public void pingViaSSH() throws CDLException, IOException {
		Cout.debugSuperVisible("Starting test");

		// creates a component in this JVM
		Component c1 = new Component();

		// and deploy another one in a separate JVM
		// they will communicate through standard streams
		ComponentInfo c2 = ComponentInfo.fromCDL("name=c2 / ssh=" + ssh);

		c1.lookupService(ComponentDeployer.class).deploy(Set.of(c2), true, 10, true, fdbck -> System.out.println(fdbck),
				p -> System.out.println("ok"));

		// asks the master to ping the other component
		Message pong = c1.lookupService(PingPong.class).ping(c2, 5);
		System.out.println("pong: " + pong);

// be sure it got an answer
		assertNotEquals(null, pong);

		// clean
		Component.componentsInThisJVM.clear();
	}
}
