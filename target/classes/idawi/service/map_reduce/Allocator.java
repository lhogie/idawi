package idawi.service.map_reduce;

import java.util.List;

import idawi.Component;

public interface Allocator<R> {
	void assign(List<Task<R>> tasks, List<Component> workers);
}