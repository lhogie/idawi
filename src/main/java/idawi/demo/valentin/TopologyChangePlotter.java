package idawi.demo.valentin;

import java.util.function.Function;

import idawi.Component;
import idawi.service.local_view.NetworkTopologyListener;
import idawi.transport.Link;

public class TopologyChangePlotter implements NetworkTopologyListener {

	final Component c;
	private Function<Integer, Boolean> ok;
	private int i = 0;

	public TopologyChangePlotter(Component c, Function<Integer, Boolean> ok) {
		this.c = c;
		c.localView().g.listeners.add(this);
		this.ok = ok;
	}

	@Override
	public void newComponent(Component p) {
		plot();
	}

	@Override
	public void linkDeactivated(Link l) {
		plot();
	}

	@Override
	public void linkActivated(Link l) {
		plot();
	}

	@Override
	public void componentHasGone(Component a) {
		plot();
	}

	private void plot() {
		if (ok.apply(i++)) {
			c.localView().g.plot();
		}

	}
}
