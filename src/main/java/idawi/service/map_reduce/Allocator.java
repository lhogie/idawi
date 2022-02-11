package idawi.service.map_reduce;

import java.util.List;

import idawi.ComponentDescriptor;

public interface Allocator<R> {
	void assign(List<Task<R>> tasks, List<ComponentDescriptor> workers);
}