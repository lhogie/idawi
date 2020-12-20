package idawi;

import java.util.HashSet;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import idawi.net.LMI;
import idawi.service.PingPong;

public class All2all {
	public static void main(String[] args) throws RemoteException {
		new All2all().all2all();
	}

	@Test
	public void all2all() throws RemoteException {
		MessageQueue.DEFAULT_TIMEOUT_IN_SECONDS = 1;
		var all = new HashSet<ComponentInfo>();

		System.out.println("Creating components");
		for (int i = 0; i < 35; ++i) {
			var c = new Component();
			all.add(c.descriptor());
		}

		System.out.println("Connecting them");
//		LMI.randomTree(new ArrayList<>(Component.componentsInThisJVM.values()));
		LMI.clique(Component.componentsInThisJVM.values());

		System.out.println("messaging");
		AtomicLong n = new AtomicLong();

		for (var c : Component.componentsInThisJVM.values()) {
			var allButMe = new HashSet<>(all);
			allButMe.remove(c.descriptor());
			System.out.println(c + " pings " + allButMe);
			c.lookupService(PingPong.class).ping(allButMe).forEach2(r -> {
				n.incrementAndGet();
				System.out.println(n.get() + ": " + r);
			});
			break;
		}

		System.out.println("done");
		Component.componentsInThisJVM.clear();
	}
}
