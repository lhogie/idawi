package idawi.routing;

import java.io.PrintStream;
import java.util.stream.Stream;

import idawi.Component;
import idawi.messaging.Message;

public interface RoutingListener {

	public final static RoutingListener stdout = new RoutingListener.PrintTo(System.out);

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

	public final class PrintTo implements RoutingListener {
		public final PrintStream out;

		public PrintTo(PrintStream o) {
			this.out = o;
		}

		@Override
		public void messageDropped(RoutingService s, Message msg) {
			print(s + " *** dropped: " + Long.toHexString(msg.ID));
		}

		protected void print(Object o) {
			out.println("routing: " + o);
		}

		@Override
		public void messageForwarded(RoutingService s, Message msg) {
			// out.println(s + " *** forwards " + Long.toHexString(msg.ID));
			print(s + " *** forwarded: " + msg);
		}
	};
}
