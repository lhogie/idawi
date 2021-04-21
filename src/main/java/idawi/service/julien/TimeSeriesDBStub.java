package idawi.service.julien;

import java.awt.Color;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

import idawi.ComponentAddress;
import idawi.OperationParameterList;
import idawi.Service;
import idawi.ServiceAddress;
import idawi.ServiceStub;
import xycharter.Figure;

public class TimeSeriesDBStub extends ServiceStub {
	private final PointBuffer buf = new PointBuffer();

	public TimeSeriesDBStub(Service localService, ComponentAddress remoteComponents) {
		super(localService, new ServiceAddress(remoteComponents, TimeSeriesDB.class));
	}

	public void registerPoint(String metricName, double x, double y, double uploadProbability) throws Throwable {
		buf.add(metricName, x, y);

		if (Math.random() < uploadProbability) {
			sendBuffer();
		}
	}

	public void sendBuffer() throws Throwable {
		localService.exec(to, TimeSeriesDB.addPoint, true, buf).returnQ.collect().throwAnyError();
		buf.clear();
	}

	public String subscribe(Set<String> metricNames, Consumer<byte[]> newImage) throws Throwable {
		Subscribe s = new Subscribe();
		s.metricNames = metricNames;
		s.id = "subscribe_" + ThreadLocalRandom.current().nextLong();
		localService.registerOperation(s.id, (msg, returns) -> newImage.accept((byte[]) msg.content));
		localService.exec(to, TimeSeriesDB.getPlot_subscribe, true, s).returnQ.collect().throwAnyError();
		return s.id;
	}

	public void subscribe(String id, Consumer<byte[]> newImage) throws Throwable {
		localService.exec(to, TimeSeriesDB.getPlot_unsubscribe, true, id).returnQ.collect().throwAnyError();
	}

	public void createFigure(String metricName) throws Throwable {
		localService.exec(to, TimeSeriesDB.createFigure, true, metricName).returnQ.collect().throwAnyError();
	}

	public void setFigureColor(String metricName, Color c) throws Throwable {
		localService.exec(to, TimeSeriesDB.setFigureColor, true, new OperationParameterList(metricName, c)).returnQ.collect()
				.throwAnyError();
	}

	public byte[] getPlot(Set<String> metricNames, String title, String format) throws Throwable {
		return (byte[]) localService.exec(to, TimeSeriesDB.getPlot, true,
				new OperationParameterList(metricNames, title, format)).returnQ.get();
	}

	public byte[] getPlot_subscribe(Set<String> metricNames, String title, String format) throws Throwable {
		return (byte[]) localService.exec(to, TimeSeriesDB.getPlot, true,
				new OperationParameterList(metricNames, title, format)).returnQ.get();
	}

	public Set<String> metricNames() throws Throwable {
		return (Set<String>) localService.exec(to, TimeSeriesDB.getMetricNames, true, new OperationParameterList()).returnQ.get();
	}

	public Figure download(String figureName) throws Throwable {
		return (Figure) localService.exec(to, TimeSeriesDB.retrieveFigure, true, figureName).returnQ.get();
	}
}
