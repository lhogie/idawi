package idawi.service.map_reduce;

import java.util.List;
import java.util.Random;
import java.util.Set;

import idawi.knowledge_base.ComponentRef;
import idawi.routing.TargetComponents;

public class RandomAllocator<R> implements Allocator<R> {
	@Override
	public void assign(List<Task<R>> tasks, List<ComponentRef> workers) {
		var r = new Random();
		tasks.forEach(t -> t.to = new TargetComponents.Multicast(Set.of(workers.get(r.nextInt(workers.size())))));
	}
}