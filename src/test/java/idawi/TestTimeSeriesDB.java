package idawi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import org.junit.jupiter.api.Test;

import idawi.service.ComponentDeployer;
import idawi.service.ServiceManager;
import idawi.service.julien.TimeSeriesDB;
import idawi.service.julien.TimeSeriesDBStub;
import xycharter.Figure;

public class TestTimeSeriesDB {

	public static void main(String[] args) throws Throwable {
		new TestTimeSeriesDB().startService();
	}

	@Test
	public void startService() throws Throwable {
		Component c1 = new Component();
		ComponentDescriptor c2 = c1.lookupService(ComponentDeployer.class).deployLocalPeers(1, i -> "other-" + i, true, null)
				.iterator().next().descriptor();

		Service s = new Service(c1);

		new ServiceManager.Stub(s, Set.of(c2)).start(TimeSeriesDB.class);

		TimeSeriesDBStub client = new TimeSeriesDBStub(s, Set.of(c2));

		client.createFigure("fig1");

		for (int i = 0; i < 10; ++i) {
			client.registerPoint("fig1", i, Math.random(), 1);
		}

		var names = client.metricNames();
		assertEquals(names.iterator().next(), "fig1");

		Figure f = client.download("fig1");
		assertEquals(f.getNbPoints(), 10);

		Component.componentsInThisJVM.clear();
		Component.stopPlatformThreads();
	}
}