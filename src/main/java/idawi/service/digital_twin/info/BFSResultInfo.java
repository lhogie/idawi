package idawi.service.digital_twin.info;

import java.util.function.Predicate;

import idawi.Component;
import idawi.service.local_view.Info;
import idawi.service.local_view.BFS.BFSResult;

public class BFSResultInfo extends Info {
	BFSResult r;

	public BFSResultInfo(double date, BFSResult r) {
		super(date);
		this.r = r;
	}

	@Override
	public String toString() {
		return "BFS [distances=" + r.distances + "]";
	}

	@Override
	public void exposeComponent(Predicate<Component> p) {
		for (var c : r.distances.keySet()) {
			if (p.test(c)) {
				return;
			}
		}
	}

}
