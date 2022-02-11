package idawi.service.map_reduce;

import java.util.List;
import java.util.Set;

import idawi.ComponentDescriptor;
import idawi.To;

public class RoundRobinAllocator<R> implements Allocator<R> {
	@Override
	public void assign(List<Task<R>> tasks, List<ComponentDescriptor> workers) {
		tasks.forEach(t -> t.to = new To(Set.of(workers.get(t.id % workers.size()))));
	}
}