package idawi.service.map_reduce;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import idawi.Component;
import idawi.ComponentDescriptor;
import idawi.InnerOperation;
import idawi.Message;
import idawi.MessageQueue;
import idawi.MessageQueue.Enough;
import idawi.OperationParameterList;
import idawi.ProgressMessage;
import idawi.Service;
import idawi.To;
import idawi.service.DeployerService;
import idawi.service.ServiceManager;
import toools.thread.AtomicDouble;
import toools.util.Date;

public class MapReduce extends Service {
	public static abstract class Task<R> implements Serializable {
		// valid at a given round only
		private int id;
		private transient MapReduce mapReduceService;
		private transient To to;

		public abstract R compute(Consumer output) throws Throwable;
	}

	public static class Result<R> implements Serializable {
		public int taskID;
		public R value;
		public double receptionDate;
		public double completionDate;
		public ComponentDescriptor worker;
	}

	static interface ResultHandler<R> {
		void newResult(Result<R> newResult);

		void newProgressMessage(String msg);

		void newProgressRatio(double r);

		void newMessage(Message a);
	}

	public MapReduce(Component component) {
		super(component);
		operations.add(new taskProcessor());
	}

	// the backend op
	public class taskProcessor extends InnerOperation {

		@Override
		public String getDescription() {
			return "computes a given task assigned to it and return the result to the mapper";
		}

		@Override
		public void exec(MessageQueue in) throws Throwable {
			var msg = in.get_blocking();
			var t = (Task) msg.content;
			reply(msg, new ProgressMessage("processing task " + t.id));
			t.mapReduceService = MapReduce.this;
			Result r = new Result<>();
			r.receptionDate = Date.time();
			r.taskID = t.id;
			r.value = t.compute(something -> reply(msg, something));
			r.completionDate = Date.time();
			reply(msg, new ProgressMessage("sending result " + t.id));
			reply(msg, r);
		}
	}

	public static interface Allocator<R> {
		void assign(List<Task<R>> tasks, List<ComponentDescriptor> workers);
	}

	public static class RoundRobinAllocator<R> implements Allocator<R> {
		@Override
		public void assign(List<Task<R>> tasks, List<ComponentDescriptor> workers) {
			tasks.forEach(t -> t.to = new To(Set.of(workers.get(t.id % workers.size()))));
		}
	}

	public static class All2AllAllocator<R> implements Allocator<R> {
		@Override
		public void assign(List<Task<R>> tasks, List<ComponentDescriptor> workers) {
			tasks.forEach(t -> t.to = new To(new HashSet<>(workers)));
		}
	}

	public static class RandomAllocator<R> implements Allocator<R> {
		@Override
		public void assign(List<Task<R>> tasks, List<ComponentDescriptor> workers) {
			var r = new Random();
			tasks.forEach(t -> t.to = new To(Set.of(workers.get(r.nextInt(workers.size())))));
		}
	}

	private <R> R map(List<Task<R>> tasks, List<ComponentDescriptor> workers, BiFunction<R, R, R> f) {
		class ResultHolder {
			R result;
		}

		var h = new ResultHolder();

		map(tasks, workers, new RoundRobinAllocator<R>(),
				r -> h.result = h.result == null ? r.value : f.apply(h.result, r.value), progressMessages -> {
				}, progressRatio -> {
				}, anyOtherMessage -> {
				});

		return h.result;
	}

	public <R> Collection<Task<R>> map(List<Task<R>> tasks, List<ComponentDescriptor> workers, Allocator<R> assigner,
			Consumer<Result<R>> newResult, Consumer<String> progressMessages, Consumer<Double> progressRatio,
			Consumer<Message> otherMessages) {
		return map(tasks, workers, assigner, new ResultHandler<R>() {

			@Override
			public void newResult(Result<R> nr) {
				newResult.accept(nr);
			}

			@Override
			public void newProgressMessage(String msg) {
				progressMessages.accept(msg);
			}

			@Override
			public void newProgressRatio(double r) {
				progressRatio.accept(r);
			}

			@Override
			public void newMessage(Message a) {
				otherMessages.accept(a);
			}
		});
	}

	public <R> Collection<Task<R>> map(List<Task<R>> tasks, List<ComponentDescriptor> workers, Allocator<R> allocator,
			ResultHandler<R> h) {
		var unprocessedTasks = new ArrayList<>(tasks);

		// all results will end here
		var q = createQueue();

		// assign IDs
		IntStream.range(0, unprocessedTasks.size()).forEach(taskID -> unprocessedTasks.get(taskID).id = taskID);

		for (int round = 0; !unprocessedTasks.isEmpty(); ++round) {
			h.newProgressMessage("starting round" + round);

			if (allocator != null) {
				allocator.assign(unprocessedTasks, workers);
			}

			for (var task : tasks) {
				if (task.to != null) {
					h.newProgressMessage("sending task " + task.id + " to " + task.to);
					exec(task.to.o(taskProcessor.class), q, task);
				}
			}

			h.newProgressMessage("waiting for results");
			q.setMaxWaitTimeS(60).forEach(msg -> {
				if (msg.content instanceof Result) {
					var workerResponse = (Result<R>) msg.content;
					workerResponse.worker = msg.route.source().component;
					unprocessedTasks.removeIf(t -> t.id == workerResponse.taskID);
					h.newResult(workerResponse);
					h.newProgressRatio(100 * (tasks.size() - unprocessedTasks.size()) / (double) tasks.size());
				} else if (msg.content instanceof ProgressMessage) {
					h.newProgressMessage(msg.route.source().componentID + ": " + msg.content);
				} else {
					h.newMessage(msg);
				}

				return unprocessedTasks.isEmpty() ? Enough.yes : Enough.no;
			});
		}

		deleteQueue(q);
		return unprocessedTasks;
	}

	// that's the task we'll send to workers
	static class MyTask extends Task<Integer> {
		double minDuration = 0, maxDuration = 0;

		@Override
		public Integer compute(Consumer output) {
			// 0.1 chances that this task fails
			if (Math.random() < 0) {
				output.accept("I'm feeling bad");
				throw new Error();
			}

			// Threads.sleep((maxDuration - minDuration) * Math.random() + minDuration);
			return 1;
		}
	}

	public static void main(String[] args) throws IOException {
		Component mapper = new Component("mapper");
		var clientService = new Service(mapper);

		// create workers
		var workers = new HashSet<ComponentDescriptor>();
		IntStream.range(0, 1).forEach(i -> workers.add(mapper.descriptor("w" + i, true)));

		// deploy JVMs
		mapper.lookup(DeployerService.class).deployInNewJVMs(workers);

		// start Map/Reduce workers in them
		System.out.println("starting map/reduce service on " + workers);
		var ro = clientService.exec(new To(workers).o(ServiceManager.ensureStarted.class), true,
				new OperationParameterList(MapReduce.class));
		ro.returnQ.setMaxWaitTimeS(60).collectUntilNEOT(workers.size());

		// create tasks
		List<Task<Integer>> tasks = new ArrayList<>();
		IntStream.range(0, 10).forEach(i -> tasks.add(new MyTask()));

		final AtomicDouble finalResult = new AtomicDouble();
		var workerList = new ArrayList<>(workers);

		new MapReduce(mapper).map(tasks, workerList, new RoundRobinAllocator<Integer>(), // assign tasks to workers
				newResult -> finalResult.set(finalResult.get() + newResult.value), // reduce
				progress -> System.out.println(progress), // print progress information
				progressRatio -> System.out.println("progress: " + progressRatio + "%"), // print progress information
				msg -> System.out.println("---" + msg)); // other messages are just printed out

		new MapReduce(mapper).map(tasks, workerList, (a, b) -> a + b);

		new MapReduce(mapper).map(tasks, workerList, new RoundRobinAllocator<Integer>(), new ResultHandler<Integer>() {

			@Override
			public void newResult(Result<Integer> newResult) {
				double previousResult = finalResult.get();
				double sum = previousResult + newResult.value;
				finalResult.set(sum);
			}

			@Override
			public void newProgressMessage(String msg) {
				System.out.println("progress: " + msg);
			}

			@Override
			public void newProgressRatio(double r) {
				System.out.println("progress ratio: " + r + "%");
			}

			@Override
			public void newMessage(Message a) {
				System.out.println("---" + a.content);
			}
		});

		System.out.println("result= " + finalResult.get());

		Component.stopPlatformThreads();
	}

}
