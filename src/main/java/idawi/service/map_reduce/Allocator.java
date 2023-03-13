package idawi.service.map_reduce;

import java.util.List;

import idawi.knowledge_base.ComponentRef;

public interface Allocator<R> {
	void assign(List<Task<R>> tasks, List<ComponentRef> workers);
}