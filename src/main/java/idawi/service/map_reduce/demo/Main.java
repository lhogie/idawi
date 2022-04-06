package idawi.service.map_reduce.demo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.IntStream;

import idawi.Component;
import idawi.ComponentDescriptor;
import idawi.Message;
import idawi.OperationParameterList;
import idawi.Service;
import idawi.To;
import idawi.deploy.DeployerService;
import idawi.service.ServiceManager;
import idawi.service.map_reduce.MapReduceService;
import idawi.service.map_reduce.Result;
import idawi.service.map_reduce.ResultHandler;
import idawi.service.map_reduce.RoundRobinAllocator;
import idawi.service.map_reduce.Task;
import toools.thread.AtomicDouble;

public class Main {
	public static void main(String[] args) throws IOException {
		Component mapper = new Component("mapper");
		var clientService = new Service(mapper);

		// create workers
		var workers = new HashSet<ComponentDescriptor>();
		IntStream.range(0, 1).forEach(i -> workers.add(mapper.descriptor("w" + i, true)));

		// deploy JVMs
		mapper.lookup(DeployerService.class).deployInNewJVMs(workers, stdout -> System.out.println(stdout),
				ok -> System.out.println("peer ok: " + ok));

		// start Map/Reduce workers in them
		System.out.println("starting map/reduce service on " + workers);
		var ro = clientService.exec(new To(workers).o(ServiceManager.ensureStarted.class), true,
				new OperationParameterList(MapReduceService.class));
		ro.returnQ.collectUntilNEOT(1, workers.size());

		// create tasks
		List<Task<Integer>> tasks = new ArrayList<>();
		IntStream.range(0, 10).forEach(i -> tasks.add(new MyTask()));

		final AtomicDouble finalResult = new AtomicDouble();
		var workerList = new ArrayList<>(workers);

//		new MapReduce(mapper).map(tasks, workerList, (a, b) -> a + b);

		new MapReduceService(mapper).map(tasks, workerList, new RoundRobinAllocator<Integer>(),
				new ResultHandler<Integer>() {

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
