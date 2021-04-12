package idawi.service.julien;

import java.io.StringReader;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.apache.batik.dom.svg.SAXSVGDocumentFactory;
import org.apache.batik.swing.JSVGCanvas;
import org.apache.batik.util.XMLResourceDescriptor;
import org.w3c.dom.svg.SVGDocument;

import idawi.ComponentAddress;
import idawi.ComponentDescriptor;
import idawi.Service;
import idawi.service.DeployerService;
import idawi.service.ServiceManager;
import idawi.service.rest.RESTService;
import toools.gui.Utilities;
import toools.thread.Threads;

public class Demo {
	public static void main(String[] args) throws Throwable {
		System.out.println("start");

		new Service() {
			@Override
			public void run() throws Throwable {
				// start a new JVM to host the time series DB
				ComponentDescriptor serverDescriptor = ComponentDescriptor
						.fromCDL("name=db / udp_port=56933 / ssh=musclotte.inria.fr");
				var server = new TimeSeriesDBStub(this, new ComponentAddress(Set.of(serverDescriptor)));
				lookupService(DeployerService.class).deploy(Set.of(serverDescriptor), true, 15, false, null, null);
				exec(new ComponentAddress(Set.of(serverDescriptor)), ServiceManager.start, true, TimeSeriesDB.class);
				lookupService(RESTService.class).startHTTPServer();

				// creates the figure that will be fed
				server.createFigure("some metric");
				startGUI2(server, serverDescriptor);

				// runs the simulation
				for (int step = 0;; ++step) {
					// computes something
					Threads.sleepMs(100);
					System.out.println("sending point");
					// send point
					server.registerPoint("some metric", step, Math.random(), 1);
				}
			}
		};
	}

	private static void startGUI(TimeSeriesDBStub localDB, ComponentDescriptor remoteDB) {
		String parser = XMLResourceDescriptor.getXMLParserClassName();
		SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(parser);
		JSVGCanvas c = new JSVGCanvas();
		JFrame frame = Utilities.displayInJFrame(c, "demo for Julien");
		frame.setSize(800, 600);
		long startDate = System.currentTimeMillis();
		AtomicInteger i = new AtomicInteger();

		Threads.newThread_loop(() -> {

			try {
				String svg = new String(localDB.getPlot(Set.of("some metric"), "my first plot", "svg"));
				SVGDocument document = factory.createSVGDocument(null, new StringReader(svg));
				c.setSVGDocument(document);
			} catch (Throwable e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			double durationS = ((System.currentTimeMillis() - startDate) / 1000);

			if (durationS > 0) {
				double freq = i.get() / durationS;
				System.out.println("updated " + freq + "frame/s");
			}

			i.incrementAndGet();
		});
	}

	private static void startGUI2(TimeSeriesDBStub client, ComponentDescriptor server) {
		JLabel c = new JLabel();
		JFrame frame = Utilities.displayInJFrame(c, "demo for Julien");
		frame.setSize(800, 600);

		long startDate = System.currentTimeMillis();
		AtomicInteger i = new AtomicInteger();

		Threads.newThread_loop(() -> {
			try {
				byte[] png = client.getPlot(Set.of("some metric"), "my first plot", "png");

				if (png != null) {
					c.setIcon(new ImageIcon(png));

					double durationS = ((System.currentTimeMillis() - startDate) / 1000);

					if (durationS > 0) {
						double freq = i.get() / durationS;
						System.out.println("updated " + freq + "frame/s");
					}

				}
				i.incrementAndGet();
			} catch (Throwable e) {
				e.printStackTrace();
			}
		});
	}
}
