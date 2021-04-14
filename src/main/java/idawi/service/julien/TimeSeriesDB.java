package idawi.service.julien;

import java.awt.Color;
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

import idawi.AsMethodOperation.OperationID;
import idawi.Component;
import idawi.IdawiExposed;
import idawi.Message;
import idawi.MessageQueue;
import idawi.Service;
import idawi.Streams;
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

	public static OperationID addPoint;

	@IdawiExposed
	public void addPoint(PointBuffer buf) {
		for (Figure f : buf.values()) {
			Figure a = name2figure.get(f.name);

			if (a == null)
				throw new Error("figure not found: " + f.name);

			a.addPoints(f);
		}
	}

	public static OperationID store;

	@IdawiExposed
	public void store(String workbenchName) {
		var file = new RegularFile(baseDir, workbenchName);
		file.setContentAsJavaObject(name2figure);
	}

	public static OperationID load;

	@IdawiExposed
	public void load(String workbenchName) {
		var file = new RegularFile(baseDir, workbenchName);
		name2figure = (Map<String, Figure>) file.getContentAsJavaObject();
	}

	public static OperationID removeFigure;

	@IdawiExposed
	public void removeMetric(String figName) {
		name2figure.remove(figName);
	}

	public static OperationID getNbPoints;

	@IdawiExposed
	public int getNbPoints(String figName) {
		return name2figure.get(figName).getNbPoints();
	}

	public static OperationID getFigureList;

	@IdawiExposed
	public Set<String> getMetricNames() {
		return new HashSet<>(name2figure.keySet());
	}

	public static OperationID getWorkbenchList;

	@IdawiExposed
	public Set<String> getWorkbenchList() {
		return baseDir.listRegularFiles().stream().map(f -> f.getName()).collect(Collectors.toSet());
	}

	public static OperationID retrieveFigure;

	@IdawiExposed
	public Figure retrieveFigure(String figureName) {
		return name2figure.get(figureName);
	}

	public static OperationID retrieveWorkbench;

	@IdawiExposed
	public void retrieveWorkbench(MessageQueue in) throws Throwable {
		PipedOutputStream pos = new PipedOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(pos);
		oos.writeObject(name2figure);
		Streams.split(new PipedInputStream(pos), 1000, b -> send(b, in.get_blocking().requester));
	}

	public static OperationID createFigure;

	@IdawiExposed
	synchronized public void createFigure(String name) {
		Figure f = new Figure();
		f.setName(name);
		f.addRenderer(new ConnectedLineFigureRenderer());
		name2figure.put(name, f);
	}

	public static OperationID setFigureColor;

	@IdawiExposed
	synchronized public void setFigureColor(String figName, Color color) {
		name2figure.get(figName).setColor(color);
	}

	public static OperationID filter;

	@IdawiExposed
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

	public static OperationID getPlot;

	@IdawiExposed
	public void getPlot(Set<String> metricNames, String title, String format, Consumer<Object> returns) {
		Plot plot = new Plot();
		metricNames.forEach(n -> plot.addFigure(name2figure.get(n)));
		plot.getSpace().getLegend().setText(title);
		byte[] rawData = getPlotRawData(plot, format);
		returns.accept(rawData);
	}

	private final LongSet subscriptions = new LongOpenHashSet();

	public static OperationID getPlot_subscribe;

	@IdawiExposed
	public void getPlot_subscribe(Set<String> metricNames, String title, String format, Consumer<Object> returns) {
		Plot plot = new Plot();
		metricNames.forEach(n -> plot.addFigure(name2figure.get(n)));
		plot.getSpace().getLegend().setText(title);
		long id = ThreadLocalRandom.current().nextLong();
		subscriptions.add(id);
		returns.accept(id);
		int periodMs = 1000;
		Threads.newThread_loop_periodic(periodMs, () -> subscriptions.contains(id), () -> {
			byte[] rawData = getPlotRawData(plot, format);
			returns.accept(rawData);
		});
	}

	public static OperationID getPlot_unsubscribe;

	@IdawiExposed
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
