package idawi.service.julien;

import java.awt.Color;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

import idawi.Component;
import idawi.ComponentInfo;
import idawi.MessageException;
import idawi.Service;
import idawi.To;
import idawi.service.ComponentStub;
import idawi.service.ServiceManager;
import xycharter.Figure;

public class TimeSeriesDBClient extends Service {

	public TimeSeriesDBClient(Component t) {
		super(t);
	}

	
	private final PointBuffer buf = new PointBuffer();

	public void sendPoint(String metricName, double x, double y, ComponentInfo db, double uploadProbability)
			throws MessageException {
		buf.add(metricName, x, y);

		if (Math.random() < uploadProbability) {
			sendBuf(db);
		}
	}

	public void sendBuf(ComponentInfo db) throws MessageException {
		call(db, TimeSeriesDB.class, "addPoint", buf).collect().throwAnyError();
		buf.clear();
	}

	public String subscribe(Set<String> metricNames, ComponentInfo db, Consumer<byte[]> newImage)
			throws MessageException {
		Subscribe s = new Subscribe();
		s.metricNames = metricNames;
		s.id = "subscribe_" + ThreadLocalRandom.current().nextLong();
		registerOperation(s.id, (msg, returns) -> newImage.accept((byte[]) msg.content));
		send(s, new To(db, TimeSeriesDB.class, "getPlot_subscribe")).collect().throwAnyError();
		return s.id;
	}

	public void subscribe(String id, ComponentInfo db, Consumer<byte[]> newImage) throws MessageException {
		send(id, new To(db, TimeSeriesDB.class, "getPlot_unsubscribe")).collect().throwAnyError();
	}

	public void createFigure(String metricName, ComponentInfo db) throws MessageException {
		send(metricName, to(db, TimeSeriesDB.class, "createFigure")).collect().throwAnyError();
	}

	public void setFigureColor(String metricName, Color c, ComponentInfo db) throws MessageException {
		call(db, TimeSeriesDB.class, "setFigureColor", metricName, c).collect().throwAnyError();
	}

	public byte[] getPlot(Set<String> metricNames, String title, String format, ComponentInfo timeSeriesDB)
			throws MessageException {
		return (byte[]) call(timeSeriesDB, TimeSeriesDB.class, "getPlot", metricNames, title, format).collect()
				.throwAnyError().resultMessages().first().content;
	}

	public byte[] getPlot_subscribe(Set<String> metricNames, String title, String format, ComponentInfo timeSeriesDB)
			throws MessageException {
		return (byte[]) call(timeSeriesDB, TimeSeriesDB.class, "getPlot", metricNames, title, format).collect()
				.throwAnyError().resultMessages(1).first().content;
	}

	public Set<String> metricNames(ComponentInfo remoteTSDB) throws MessageException {
		return (Set<String>) call(remoteTSDB, TimeSeriesDB.class, "getFigureList").collect().throwAnyError()
				.resultMessages().first().content;
	}

	public Figure download(String figureName, ComponentInfo remoteTSDB) throws MessageException {
		return (Figure) call(remoteTSDB, TimeSeriesDB.class, "retrieveFigure", figureName).collect().throwAnyError()
				.getContentOrNull(0);
	}

}
