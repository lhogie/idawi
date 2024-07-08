package idawi.demo.valentin;

import idawi.Component;
import idawi.Event;
import idawi.PointInTime;
import idawi.Agenda;
import idawi.transport.TransportService;
import jexperiment.Plots;
import toools.io.file.Directory;
import toools.io.file.RegularFile;

class MeasureEvent extends Event<PointInTime> {

	protected Plots exp;
	private Component root;

	public MeasureEvent(Plots exp, Component root, double date) {
		super(new PointInTime(date));
		this.exp = exp;
		this.root = root;
	}

	@Override
	public void run() {		
		
		exp.forEachPlot(null);


	}

}