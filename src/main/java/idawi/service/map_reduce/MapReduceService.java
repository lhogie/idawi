package idawi.service.map_reduce;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import idawi.Component;
import idawi.InnerClassEndpoint;
import idawi.Service;
import idawi.messaging.Message;
import idawi.messaging.MessageQueue;
import idawi.messaging.ProgressMessage;
import toools.io.Cout;
import toools.util.Date;

public class MapReduceService extends Service {
	public MapReduceService(Component component) {
		super(component);
	}


	// the backend op
	public class taskProcessor extends InnerClassEndpoint {

		@Override
		public String getDescription() {
			return "computes a given task assigned to it and return the result to the mapper";
		}

		@Override
		public void impl(MessageQueue in) throws Throwable {
			var msg = in.poll_sync();
			Cout.debug(":) received " + msg);
			var t = (Task) msg.content;
			Cout.debug("received2 " + msg);
			reply(msg, new ProgressMessage("processing task " + t.id), false);
			t.mapReduceService = MapReduceService.this;
			Result r = new Result<>();
			r.receptionDate = Date.time();
			r.taskID = t.id;
			r.value = t.compute(something -> reply(msg, something, false));
			r.completionDate = Date.time();
			reply(msg, new ProgressMessage("sending result " + t.id), false);
			reply(msg, r, true);
		}
	}

	public <R> R map(List<Task<R>> tasks, List<Component> workers, BiFunction<R, R, R> f) {
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

	public <R> Collection<Task<R>> map(List<Task<R>> tasks, List<Component> workers, Allocator<R> assigner,
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

	public <R> Collection<Task<R>> map(List<Task<R>> tasks, List<Component> workers, Allocator<R> allocator,
			ResultHandler<R> h) {
		var unprocessedTasks = new ArrayList<>(tasks);

		// all results will end here
		var q = createUniqueQueue("results");

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
					component.defaultRoutingProtocol().exec(getClass(), taskProcessor.class, null, task.to, q, task, true);
				}
			}

			h.newProgressMessage("waiting for results");
			q.collector().collect(c -> {
				var msg = c.messages.last();
				if (msg.content instanceof Result) {
					var workerResponse = (Result<R>) msg.content;
					workerResponse.worker = msg.route.source();
					unprocessedTasks.removeIf(t -> t.id == workerResponse.taskID);
					h.newResult(workerResponse);
					h.newProgressRatio(100 * (tasks.size() - unprocessedTasks.size()) / (double) tasks.size());
				} else if (msg.content instanceof ProgressMessage) {
					h.newProgressMessage(msg.route.source() + ": " + msg.content);
				} else {
					h.newMessage(msg);
				}

				c.stop = unprocessedTasks.isEmpty();
			});
		}

		detachQueue(q);
		return unprocessedTasks;
	}

}
