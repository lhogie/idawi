package idawi.service.map_reduce;

import java.util.List;
import java.util.Set;

import idawi.Component;
import idawi.routing.TargetComponents;

public class RoundRobinAllocator<R> implements Allocator<R> {
	@Override
	public void assign(List<Task<R>> tasks, List<Component> workers) {
		tasks.forEach(t -> t.to = new TargetComponents.Multicast(Set.of(workers.get(t.id % workers.size()))));
	}
}