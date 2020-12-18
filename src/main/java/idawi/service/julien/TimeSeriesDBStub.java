package idawi.service.julien;

import java.awt.Color;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

import idawi.ComponentInfo;
import idawi.MessageException;
import idawi.Service;
import idawi.To;
import idawi.service.ComponentStub;
import xycharter.Figure;

public class TimeSeriesDBStub extends ComponentStub<TimeSeriesDB> {
	private final PointBuffer buf = new PointBuffer();

	public TimeSeriesDBStub(Service localService, ComponentInfo remoteComponent) {
		super(localService, remoteComponent);
	}

	public void sendPoint(String metricName, double x, double y, double uploadProbability) throws MessageException {
		buf.add(metricName, x, y);

		if (Math.random() < uploadProbability) {
			sendBuf();
		}
	}

	public void sendBuf() throws MessageException {
		localService.call(remoteComponent, TimeSeriesDB.class, "addPoint", buf).collect().throwAnyError();
		buf.clear();
	}

	public String subscribe(Set<String> metricNames, Consumer<byte[]> newImage) throws MessageException {
		Subscribe s = new Subscribe();
		s.metricNames = metricNames;
		s.id = "subscribe_" + ThreadLocalRandom.current().nextLong();
		localService.registerOperation(s.id, (msg, returns) -> newImage.accept((byte[]) msg.content));
		localService.send(s, new To(remoteComponent, TimeSeriesDB.class, "getPlot_subscribe")).collect()
				.throwAnyError();
		return s.id;
	}

	public void subscribe(String id, Consumer<byte[]> newImage) throws MessageException {
		localService.send(id, new To(remoteComponent, TimeSeriesDB.class, "getPlot_unsubscribe")).collect()
				.throwAnyError();
	}

	public void createFigure(String metricName) throws MessageException {
		localService.send(metricName, new To(remoteComponent, TimeSeriesDB.class, "createFigure")).collect()
				.throwAnyError();
	}

	public void setFigureColor(String metricName, Color c) throws MessageException {
		localService.call(remoteComponent, TimeSeriesDB.class, "setFigureColor", metricName, c).collect()
				.throwAnyError();
	}

	public byte[] getPlot(Set<String> metricNames, String title, String format) throws MessageException {
		return (byte[]) localService.call(remoteComponent, TimeSeriesDB.class, "getPlot", metricNames, title, format)
				.collect().throwAnyError().resultMessages().first().content;
	}

	public byte[] getPlot_subscribe(Set<String> metricNames, String title, String format) throws MessageException {
		return (byte[]) localService.call(remoteComponent, TimeSeriesDB.class, "getPlot", metricNames, title, format)
				.collect().throwAnyError().resultMessages(1).first().content;
	}

	public Set<String> metricNames() throws MessageException {
		return (Set<String>) localService.call(remoteComponent, TimeSeriesDB.class, "getFigureList").collect()
				.throwAnyError().resultMessages().first().content;
	}

	public Figure download(String figureName) throws MessageException {
		return (Figure) localService.call(remoteComponent, TimeSeriesDB.class, "retrieveFigure", figureName).collect()
				.throwAnyError().getContentOrNull(0);
	}
}
