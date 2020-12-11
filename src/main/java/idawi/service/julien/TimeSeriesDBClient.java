package idawi.service.julien;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

import idawi.Component;
import idawi.ComponentInfo;
import idawi.MessageException;
import idawi.Service;
import idawi.To;
import xycharter.Figure;

public class TimeSeriesDBClient extends Service {

	public TimeSeriesDBClient(Component t) {
		super(t);
	}

	private final PointBuffer buf = new PointBuffer();

	public void sendPoint(String metricName, double x, double y, ComponentInfo db, double uploadProbability) {
		buf.add(metricName, x, y);

		if (Math.random() < uploadProbability) {
			sendBuf(db);
		}
	}

	public void sendBuf(ComponentInfo db) {
		send(buf, new To(db, TimeSeriesDB.class, "addPoint")).collect();
		buf.clear();
	}

	public String subscribe(Set<String> metricNames, ComponentInfo db, Consumer<byte[]> newImage) {
		Subscribe s = new Subscribe();
		s.metricNames = metricNames;
		s.id = "subscribe_" + ThreadLocalRandom.current().nextLong();
		registerOperation(s.id, (msg, returns) -> newImage.accept((byte[]) msg.content));
		send(s, new To(db, TimeSeriesDB.class, "getPlot_subscribe")).collect();
		return s.id;
	}

	public void subscribe(String id, ComponentInfo db, Consumer<byte[]> newImage) {
		send(id, new To(db, TimeSeriesDB.class, "getPlot_unsubscribe")).collect();
	}

	public void createFigure(String metricName, ComponentInfo db) {
		send(metricName, to(db, TimeSeriesDB.class, "createFigure")).collect();
	}

	public byte[] getPlot(Set<String> metricNames, String title, String format, ComponentInfo timeSeriesDB) {
		PlotRequest req = new PlotRequest();
		req.figureNames = metricNames;
		req.title = title;
		req.format = format;
		try {
			return (byte[]) send(req, to(timeSeriesDB, TimeSeriesDB.class, "getPlot")).collect().throwAnyError()
					.resultMessages().first().content;
		} catch (MessageException e) {
			return null;
		}
	}

	public byte[] getPlot_subscribe(Set<String> metricNames, String title, String format, ComponentInfo timeSeriesDB) {
		PlotRequest req = new PlotRequest();
		req.figureNames = metricNames;
		req.title = title;
		req.format = format;
		return (byte[]) send(req, to(timeSeriesDB, TimeSeriesDB.class, "getPlot")).collect().resultMessages(1)
				.first().content;
	}

	public Figure download(String figureName, ComponentInfo remoteTSDB) {
		return (Figure) send(figureName, to(remoteTSDB, TimeSeriesDB.class, "retrieveFigure")).collect()
				.getContentOrNull(0);
	}

}
