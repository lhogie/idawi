package idawi.service.julien;

import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import idawi.Component;
import idawi.Message;
import idawi.Operation;
import idawi.Service;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import toools.io.file.Directory;
import toools.io.file.RegularFile;
import toools.thread.Threads;
import xycharter.Figure;
import xycharter.PNGPlotter;
import xycharter.Plot;
import xycharter.render.ConnectedLineFigureRenderer;

/**
 * Sends an empty message on a queue that is created specifically for the peer
 * to bench.
 */

public class TimeSeriesDB extends Service {

	private Map<String, Figure> name2figure = new HashMap<>();
	private Directory baseDir = new Directory("$HOME/.timeSeriesDB");

	public TimeSeriesDB(Component node) {
		super(node);
	}

	@Operation
	public void addPoint(PointBuffer buf) {
		for (Figure f : buf.values()) {
			Figure a = name2figure.get(f.name);

			if (a == null)
				throw new Error("figure not found: " + f.name);

			a.addPoints(f);
		}
	}

	@Operation
	public void store(String workbenchName) {
		var file = new RegularFile(baseDir, workbenchName);
		file.setContentAsJavaObject(name2figure);
	}

	@Operation
	public void load(String workbenchName) {
		var file = new RegularFile(baseDir, workbenchName);
		name2figure = (Map<String, Figure>) file.getContentAsJavaObject();
	}

	@Operation
	public void removeFigure(String figName) {
		name2figure.remove(figName);
	}

	@Operation
	public int getNbPoints(Message msg, Consumer<Object> returns) {
		return name2figure.get((String) msg.content).getNbPoints();
	}

	@Operation
	public Set<String> getFigureList() {
		return new HashSet<>(name2figure.keySet());
	}

	@Operation
	public void getWorkbenchList(Message msg, Consumer<Object> returns) {
		returns.accept(baseDir.listRegularFiles().stream().map(f -> f.getName()).collect(Collectors.toSet()));
	}

	@Operation
	public Figure retrieveFigure(String figureName) {
		return name2figure.get(figureName);
	}

	@Operation
	public InputStream retrieveWorkbench() throws Throwable {
		PipedOutputStream pos = new PipedOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(pos);
		oos.writeObject(name2figure);
		return new PipedInputStream(pos);
	}

	@Operation
	synchronized public void createFigure(Message msg, Consumer<Object> returns) {
		String name = (String) msg.content;
		Figure f = new Figure();
		f.setName(name);
		f.addRenderer(new ConnectedLineFigureRenderer());
		name2figure.put(name, f);
	}

	@Operation
	synchronized public Set<Figure> filter(Message msg, Consumer<Object> returns) {
		Filter filter = (Filter) msg.content;
		Set<Figure> r = new HashSet<>();

		for (Figure f : name2figure.values()) {
			if (filter.fp.accept(f.name)) {
				Figure subFigure = new Figure();
				subFigure.name = f.name;
				int sz = f.getNbPoints();

				for (int i = 0; i < sz; ++i) {
					double x = f.x(i), y = f.y(i);

					if (filter.pp.accept(x, y)) {
						subFigure.addPoint(x, y);
					}
				}

				r.add(f);
			}
		}

		return r;
	}

	@Operation
	public void getPlot(Message msg, Consumer<Object> returns) {
		PlotRequest req = (PlotRequest) msg.content;
		int nnn = 0;
		Plot plot = new Plot();
		req.figureNames.forEach(n -> plot.addFigure(name2figure.get(n)));
		plot.getSpace().getLegend().setText(req.title);
		byte[] rawData = getPlotRawData(plot, req.format);
		returns.accept(rawData);
	}

	private final LongSet subscriptions = new LongOpenHashSet();

	@Operation
	public void getPlot_subscribe(Message msg, Consumer<Object> returns) {
		PlotRequest req = (PlotRequest) msg.content;
		Plot plot = new Plot();
		req.figureNames.forEach(n -> plot.addFigure(name2figure.get(n)));
		plot.getSpace().getLegend().setText(req.title);
		long id = ThreadLocalRandom.current().nextLong();
		subscriptions.add(id);
		returns.accept(id);
		int periodMs = 1000;
		Threads.newThread_loop_periodic(periodMs, () -> subscriptions.contains(id), () -> {
			byte[] rawData = getPlotRawData(plot, req.format);
			returns.accept(rawData);
		});
	}

	@Operation
	private void getPlot_unsubscribe(Message msg, Consumer<Object> returns) {
		subscriptions.remove(((Long) msg.content).longValue());
	}

	private byte[] getPlotRawData(Plot plot, String format) {
		if (format.equalsIgnoreCase("svg")) {
			return SVGPlotter.plot(plot).getBytes();
		} else if (format.equalsIgnoreCase("png")) {
			return new PNGPlotter().plot(plot);
		}

		throw new IllegalArgumentException("unknow image format: " + format);
	}

}
