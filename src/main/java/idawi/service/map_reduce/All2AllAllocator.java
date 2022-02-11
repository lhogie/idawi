package idawi.service.map_reduce;

import java.util.HashSet;
import java.util.List;

import idawi.ComponentDescriptor;
import idawi.To;

public class All2AllAllocator<R> implements Allocator<R> {
	@Override
	public void assign(List<Task<R>> tasks, List<ComponentDescriptor> workers) {
		tasks.forEach(t -> t.to = new To(new HashSet<>(workers)));
	}
}