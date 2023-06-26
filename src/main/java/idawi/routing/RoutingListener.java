package idawi.routing;

import java.io.PrintStream;
import java.util.stream.Stream;

import idawi.Component;
import idawi.messaging.Message;

public interface RoutingListener {

	static RoutingListener stdout = new RoutingListener.Stdout();

	public static void debug_on(Component... cc) {
		debug_on(Stream.of(cc));
	}

	public static void debug_on(Stream<Component> cc) {
		cc.flatMap(c -> c.services(BlindBroadcasting.class).stream()).forEach(r -> r.listeners.add(stdout));
	}

	public static void debug_off(Component... cc) {
		Stream.of(cc).flatMap(c -> c.services(BlindBroadcasting.class).stream())
				.forEach(r -> r.listeners.remove(stdout));
	}

	void messageDropped(RoutingService s, Message msg);

	void messageForwarded(RoutingService s, Message msg);

	public final class Stdout implements RoutingListener {
		PrintStream out = System.out;

		@Override
		public void messageDropped(RoutingService s, Message msg) {
			out.println(s + " *** drops " + Long.toHexString(msg.ID));
		}

		@Override
		public void messageForwarded(RoutingService s, Message msg) {
			// out.println(s + " *** forwards " + Long.toHexString(msg.ID));
			out.println(s + " *** forwards " + msg);
		}
	};
}
