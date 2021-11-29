package idawi.service;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Function;

import idawi.Component;
import idawi.ComponentAddress;
import idawi.InnerClassTypedOperation;
import idawi.QueueAddress;
import idawi.RemotelyRunningOperation;
import idawi.Service;

public class MapReduce extends Service {

	public static abstract class Task<R> implements Callable<R>, Serializable {
		public int id;
		public QueueAddress returnAddress;
	}

	public static class Result<R> implements Serializable {
		public int taskID;
		public R value;
	}

	public class Process extends InnerClassTypedOperation {

		@Override
		public String getDescription() {
			return "compute the tasks assigned to it and return the result to the mapper";
		}

		public <R> void f(Task<R> t) throws Exception {
			Result<R> r = new Result<>();
			r.taskID = t.id;
			r.value = t.call();
			send(r, t.returnAddress);
		}
	}

	Process process = new Process();

	public <R> R map(List<Task<R>> tasks, Collection<String> hosts, Function<List<Object>, R> f) {
		var hh = hosts.iterator();
		var r = new ArrayList<RemotelyRunningOperation>();
		var returnQueue = createQueue("" + Math.random(), null);
		var resultAddress = component.getAddress().s(MapReduce.class).q(returnQueue.name);

		for (int i = 0; i < tasks.size(); ++i) {
			var t = tasks.get(i);
			t.id = i;
			t.returnAddress = resultAddress;

			if (!hh.hasNext()) {
				hh = hosts.iterator();
			}

			var h = hh.next();

			r.add(start(new ComponentAddress(h), true, 1));
		}

		return f.apply(returnQueue.collect().contents());
	}

	public static void main(String[] args) throws IOException {
		Component a = new Component();
		String[] children = new String[] { "b", "c", "d", "e" };
		a.lookupService(DeployerService.class).deployOtherJVM(children);

		List<Task<Double>> tasks = new ArrayList<>();

		for (int i = 0; i < 100; ++i) {
			var t = new Task<Double>() {

				@Override
				public Double call() throws Exception {
					return Math.random();
				}
			};
		}

		a.lookupService(MapReduce.class).map(tasks, children, r -> {
			double sum = 0;

			for (double d : r) {
				sum += d;
			}

			return sum / r.size();
		});
	}
}
