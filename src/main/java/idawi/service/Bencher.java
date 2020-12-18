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
import idawi.ComponentInfo;
import idawi.MessageQueue.SUFFICIENCY;
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
		registerOperation(null, (msg, returns) -> {
			Arguments parms = (Arguments) msg.content;
			localBench(parms.size, r -> returns.accept(r));
		});
	}

	@Override
	public String getFriendlyName() {
		return "bench compuational speed";
	}

	// client
	public Map<ComponentInfo, Results> bench(Set<ComponentInfo> peers, int size,
			BiConsumer<ComponentInfo, String> msg) {
		Arguments parms = new Arguments();
		parms.size = size;
		To to = new To();
		to.notYetReachedExplicitRecipients = peers;
		to.service = id;
		Map<ComponentInfo, Results> map = new HashMap<>();

		send(parms, to).forEach(r -> {
			if (r.content instanceof String) {
				msg.accept(r.route.source().component, (String) r.content);
			} else if (r.content instanceof Results) {
				map.put(r.route.source().component, (Results) r.content);
			}

			return SUFFICIENCY.NOT_ENOUGH;
		});

		return map;
	}

	public static Results localBench(int size) {
		Results r = new Results();
		Q<Object> q = new Q<>(4);
		localBench(size, out -> {
			q.add_blocking(out);
		});

		q.get_blocking();
		r.monothread = (Long) q.get_blocking();
		q.get_blocking();
		r.multithread = (Long) q.get_blocking();
		return r;
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
