package idawi.service.julien;

import java.awt.Color;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

import idawi.ComponentInfo;
import idawi.Service;
import idawi.service.ServiceStub;
import xycharter.Figure;

public class TimeSeriesDBStub extends ServiceStub {
	private final PointBuffer buf = new PointBuffer();

	public TimeSeriesDBStub(Service localService, Set<ComponentInfo> remoteComponents) {
		super(localService, remoteComponents, TimeSeriesDB.class);
	}

	public void registerPoint(String metricName, double x, double y, double uploadProbability) throws Throwable {
		buf.add(metricName, x, y);

		if (Math.random() < uploadProbability) {
			sendBuffer();
		}
	}

	public void sendBuffer() throws Throwable {
		localService.call(to("addPoint"), buf).collect().throwAnyError();
		buf.clear();
	}

	public String subscribe(Set<String> metricNames, Consumer<byte[]> newImage) throws Throwable {
		Subscribe s = new Subscribe();
		s.metricNames = metricNames;
		s.id = "subscribe_" + ThreadLocalRandom.current().nextLong();
		localService.registerOperation(s.id, (msg, returns) -> newImage.accept((byte[]) msg.content));
		localService.send(s, to("getPlot_subscribe")).collect().throwAnyError();
		return s.id;
	}

	public void subscribe(String id, Consumer<byte[]> newImage) throws Throwable {
		localService.send(id, to("getPlot_unsubscribe")).collect().throwAnyError();
	}

	public void createFigure(String metricName) throws Throwable {
		localService.send(metricName, to("createFigure")).collect().throwAnyError();
	}

	public void setFigureColor(String metricName, Color c) throws Throwable {
		localService.call(to("setFigureColor"), metricName, c).collect().throwAnyError();
	}

	public byte[] getPlot(Set<String> metricNames, String title, String format) throws Throwable {
		return (byte[]) localService.call(to("getPlot"), metricNames, title, format).get();
	}

	public byte[] getPlot_subscribe(Set<String> metricNames, String title, String format) throws Throwable {
		return (byte[]) localService.call(to("getPlot"), metricNames, title, format).get();
	}

	public Set<String> metricNames() throws Throwable {
		return (Set<String>) localService.call(to("getFigureList")).get();
	}

	public Figure download(String figureName) throws Throwable {
		return (Figure) localService.call(to("retrieveFigure"), figureName).get();
	}
}
