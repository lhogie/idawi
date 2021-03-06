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

import idawi.AsMethodOperation.OperationID;
import idawi.Component;
import idawi.ComponentDescriptor;
import idawi.MessageQueue.SUFFICIENCY;
import idawi.Service;
import idawi.ServiceAddress;
import idawi.service.Bencher.Results;
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
		registerOperation(null, in -> {
			var msg = in.get_blocking();
			Arguments parms = (Arguments) msg.content;
			localBench(parms.size, r -> reply(msg, r));
		});
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
		var to = new ServiceAddress(peers, Bencher.class);
		Map<ComponentDescriptor, Results> map = new HashMap<>();

		trigger(to, new OperationID(id, null), true, parms).returnQ.forEach(r -> {
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
