package idawi.service;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import idawi.Component;
import idawi.FunctionEndPoint;
import idawi.InnerClassEndpoint;
import idawi.Service;
import idawi.messaging.MessageQueue;
import idawi.messaging.ProgressMessage;
import idawi.routing.ComponentMatcher;
import toools.text.json.JSONElement;
import toools.text.json.JSONable;
import toools.thread.Q;

/**
 * Sends an empty message on a queue that is created specifically for the peer
 * to bench.
 */

public class Bencher extends Service {

	public static class Arguments implements Serializable {
		public Arguments(List<String> args) {
			this.size = Integer.valueOf(args.remove(0));
		}

		public Arguments() {
			// TODO Auto-generated constructor stub
		}

		public int size;
	}

	public static class Results implements Serializable, JSONable {
		long multithread;
		long monothread;

		@Override
		public JSONElement toJSONElement() {
			return null;
		}
	}

	public Bencher(Component node) {
		super(node);
	}

	@Override
	public String getFriendlyName() {
		return "bench compuational speed";
	}

	// client
	public Map<Component, Results> bench(Set<Component> peers, int size, BiConsumer<Component, String> msg) {
		Arguments parms = new Arguments();
		parms.size = size;
		Map<Component, Results> map = new HashMap<>();

		exec(ComponentMatcher.all, getClass(), localBench.class, m -> {
		}).returnQ.collector().collect(c -> {
			var m = c.messages.last();

			if (m.content instanceof String) {
				msg.accept(m.route.source(), (String) m.content);
			} else if (m.content instanceof Results) {
				map.put(m.route.source(), (Results) m.content);
			}
		});

		return map;
	}

	public class localBench extends FunctionEndPoint<Integer, Results> {

		@Override
		public String getDescription() {
			return null;
		}

		@Override
		public Results f(Integer size) {
			Results r = new Results();
			Q<Object> q = new Q<>(4);

			localBench(size, out -> {
				if (!(out instanceof ProgressMessage)) {
					q.add_sync(out);
				}
			});

			q.poll_sync();
			r.monothread = (long) q.poll_sync();
			q.poll_sync();
			r.multithread = (long) q.poll_sync();
			return r;
		}
	}

	public class localBench2 extends InnerClassEndpoint<Integer, Results> {

		@Override
		public void impl(MessageQueue in) {
			var m = in.poll_sync();
			int size = getInputFrom(m);
			localBench(size, r -> send(r, m.replyTo));
		}

		@Override
		public String getDescription() {
			return null;
		}
	}

	public static void localBench(int size, Consumer<Object> out) {
		int[] b = new int[size];

		for (int i = 0; i < b.length; ++i) {
			b[i] = ThreadLocalRandom.current().nextInt();
		}

		{
			out.accept(new ProgressMessage("monothread sorting"));
			long startNs = System.nanoTime();
			Arrays.sort(b, 0, b.length);
			out.accept(System.nanoTime() - startNs);
		}

		for (int i = 0; i < b.length; ++i) {
			b[i] = ThreadLocalRandom.current().nextInt();
		}

		{
			out.accept(new ProgressMessage("multithread sorting"));
			long startNs = System.nanoTime();
			Arrays.parallelSort(b, 0, b.length);
			out.accept(System.nanoTime() - startNs);
		}
	}
}
