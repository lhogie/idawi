package idawi.service.map_reduce.demo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import idawi.Component;
import idawi.EndpointParameterList;
import idawi.Idawi;
import idawi.deploy.DeployerService;
import idawi.messaging.Message;
import idawi.routing.ComponentMatcher;
import idawi.service.ServiceManager;
import idawi.service.map_reduce.MapReduceService;
import idawi.service.map_reduce.Result;
import idawi.service.map_reduce.ResultHandler;
import idawi.service.map_reduce.RoundRobinAllocator;
import idawi.service.map_reduce.Task;
import toools.thread.AtomicDouble;

public class Main {
	public static void main(String[] args) throws IOException {
		Component mapper = new Component();

		DeployerService deployer = mapper.service(DeployerService.class, true);
		// create workers
		var workers = IntStream.range(0, 5).mapToObj(i -> deployer.newLocalJVM()).toList();

		// start Map/Reduce workers in them
		System.out.println("starting map/reduce service on " + workers);
		var ro = mapper.defaultRoutingProtocol().exec(ServiceManager.class, ServiceManager.ensureStarted.class, null,
				ComponentMatcher.multicast(workers), true, new EndpointParameterList(MapReduceService.class), true);
		ro.returnQ.collector().collectUntilNEOT(1, workers.size());

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

		Idawi.agenda.threadPool.shutdown();
	}

}
