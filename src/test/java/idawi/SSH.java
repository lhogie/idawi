package idawi;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.IOException;
import java.util.Set;

import org.junit.jupiter.api.Test;

import idawi.net.NetworkingService;
import idawi.service.DeployerService;
import idawi.service.PingService;
import toools.io.Cout;

public class SSH {
	public static void main(String[] args) throws Exception {
		new SSH().pingViaSSH();
	}

	public static final String ssh = "musclotte";

	@Test
	public void pingViaSSH() throws CDLException, IOException {
		Cout.debugSuperVisible("Starting test");
		NetworkingService.debug = false;

		// creates a component in this JVM
		Component c1 = new Component();

		// and deploy another one in a separate JVM
		// they will communicate through standard streams
		ComponentDescriptor c2 = ComponentDescriptor.fromCDL("name=c2 / ssh=" + ssh);

		c1.lookup(DeployerService.class).deploy(Set.of(c2), true, 10, true, fdbck -> System.out.println(fdbck),
				p -> System.out.println("ok"));

		// asks the master to ping the other component
		Message pong = c1.lookup(PingService.class).ping(c2, 5);
		System.out.println("pong: " + pong);

		// be sure it got an answer
		assertNotEquals(null, pong);

		// clean
		Component.componentsInThisJVM.clear();
	}
}
