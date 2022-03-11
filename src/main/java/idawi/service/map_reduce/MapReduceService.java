package idawi.service.map_reduce;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import idawi.Component;
import idawi.ComponentDescriptor;
import idawi.InnerOperation;
import idawi.Message;
import idawi.MessageQueue;
import idawi.MessageQueue.Enough;
import idawi.ProgressMessage;
import idawi.Service;
import toools.io.Cout;
import toools.util.Date;

public class MapReduceService extends Service {
	public MapReduceService(Component component) {
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
			Cout.debug(":) received " + msg );
			var t = (Task) msg.content;
			Cout.debug("received2 " + msg );
			reply(msg, new ProgressMessage("processing task " + t.id));
			t.mapReduceService = MapReduceService.this;
			Result r = new Result<>();
			r.receptionDate = Date.time();
			r.taskID = t.id;
			r.value = t.compute(something -> reply(msg, something));
			r.completionDate = Date.time();
			reply(msg, new ProgressMessage("sending result " + t.id));
			reply(msg, r);
		}
	}

	public <R> R map(List<Task<R>> tasks, List<ComponentDescriptor> workers, BiFunction<R, R, R> f) {
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
			q.forEach(60, msg -> {
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


}