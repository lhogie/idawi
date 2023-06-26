package idawi.service.map_reduce;

import java.util.List;

import idawi.Component;
import idawi.routing.ComponentMatcher;

public class RoundRobinAllocator<R> implements Allocator<R> {
	@Override
	public void assign(List<Task<R>> tasks, List<Component> workers) {
		tasks.forEach(t -> t.to =  ComponentMatcher.unicast(workers.get(t.id % workers.size())));
	}
}