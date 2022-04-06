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
import idawi.ComponentDescriptor;
import idawi.InnerOperation;
import idawi.TypedInnerOperation;
import idawi.MessageQueue;
import idawi.MessageQueue.Enough;
import idawi.Service;
import idawi.To;
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

	public static class Results implements Serializable {
		long monothread;
		long multithread;
	}

	public Bencher(Component node) {
		super(node);
		registerOperation(new localBench());
	}

	@Override
	public String getFriendlyName() {
		return "bench compuational speed";
	}

	// client
	public Map<ComponentDescriptor, Results> bench(Set<ComponentDescriptor> peers, int size,
			BiConsumer<ComponentDescriptor, String> msg) {
		Arguments parms = new Arguments();
		parms.size = size;
		var to = new To(peers).s(Bencher.class).o("default");
		Map<ComponentDescriptor, Results> map = new HashMap<>();

		exec(to, createQueue(), parms).returnQ.collect(c -> {
			var r = c.messages.last();
			if (r.content instanceof String) {
				msg.accept(r.route.source().component, (String) r.content);
			} else if (r.content instanceof Results) {
				map.put(r.route.source().component, (Results) r.content);
			}
		});

		return map;
	}

	public class localBench extends TypedInnerOperation {
		public Results f(int size) {
			Results r = new Results();
			Q<Object> q = new Q<>(4);
			localBench(size, out -> {
				q.add_sync(out);
			});

			q.poll_sync();
			r.monothread = (Long) q.poll_sync();
			q.poll_sync();
			r.multithread = (Long) q.poll_sync();
			return r;
		}

		@Override
		public String getDescription() {
			return null;
		}

	}

	public class localBench2 extends InnerOperation {

		@Override
		public void exec(MessageQueue in) throws Throwable {
			var m = in.poll_sync();
			int size = (int) m.content;
			localBench(size, r -> reply(m, r));
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
			out.accept("monothread sorting");
			long startNs = System.nanoTime();
			Arrays.sort(b, 0, b.length);
			out.accept(System.nanoTime() - startNs);
		}

		for (int i = 0; i < b.length; ++i) {
			b[i] = ThreadLocalRandom.current().nextInt();
		}

		{
			out.accept("multithread sorting");
			long startNs = System.nanoTime();
			Arrays.parallelSort(b, 0, b.length);
			out.accept(System.nanoTime() - startNs);
		}
	}
}
