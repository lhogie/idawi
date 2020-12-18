import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import idawi.CDLException;
import idawi.Component;
import idawi.ComponentInfo;
import idawi.MessageException;
import idawi.service.ComponentDeployer;
import idawi.service.ServiceManager;
import idawi.service.ServiceManager.Stub;
import idawi.service.julien.TimeSeriesDB;
import idawi.service.julien.TimeSeriesDBClient;
import xycharter.Figure;

public class TestTimeSeriesDB {

	public static void main(String[] args) throws CDLException, MessageException {
		new TestTimeSeriesDB().startService();
	}

	@Test
	public void startService() throws CDLException, MessageException {
		Component c1 = new Component();
		ComponentInfo c2 = c1.lookupService(ComponentDeployer.class).deployLocalPeers(1, i -> "other-" + i, true, null)
				.iterator().next().descriptor();

		
		c1.lookupService(ServiceManager.class).start(TimeSeriesDB.class, c2, 1);
		TimeSeriesDBClient client = c1.addService(TimeSeriesDBClient.class);

		client.createFigure("fig1", c2);
		
		for (int i = 0; i < 10; ++i) {
			client.sendPoint("fig1", i, Math.random(), c2, 1);
		}
		
		var names = client.metricNames(c2);
		assertEquals(names.iterator().next(), "fig1");
		
		Figure f = client.download("fig1", c2);
		assertEquals(f.getNbPoints(), 10);

		Component.componentsInThisJVM.clear();
		Component.stopPlatformThreads();
	}
}