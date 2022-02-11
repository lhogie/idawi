package idawi.service.map_reduce;

import java.util.List;
import java.util.Random;
import java.util.Set;

import idawi.ComponentDescriptor;
import idawi.To;

public class RandomAllocator<R> implements Allocator<R> {
	@Override
	public void assign(List<Task<R>> tasks, List<ComponentDescriptor> workers) {
		var r = new Random();
		tasks.forEach(t -> t.to = new To(Set.of(workers.get(r.nextInt(workers.size())))));
	}
}