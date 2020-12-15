package idawi.service.julien;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.apache.batik.dom.svg.SAXSVGDocumentFactory;
import org.apache.batik.swing.JSVGCanvas;
import org.apache.batik.util.XMLResourceDescriptor;
import org.w3c.dom.svg.SVGDocument;

import idawi.CDLException;
import idawi.Component;
import idawi.ComponentInfo;
import idawi.MessageException;
import idawi.service.ComponentDeployer;
import idawi.service.RESTService;
import idawi.service.ServiceManager;
import toools.gui.Utilities;
import toools.thread.Threads;

public class Demo {

	public static void main(String[] args) throws CDLException, IOException, MessageException {
		System.out.println("start");
		// creates a service for the communication - analogous to a TCP socket
		// loads the timeDB service locally, but just to talk to its remote peer
		Component t = new Component();
		TimeSeriesDBClient client = t.addService(TimeSeriesDBClient.class);

		// start a new JVM to host the time series DB
		Set<Component> s = new HashSet<>();
		ComponentInfo server = ComponentInfo.fromCDL("name=db / udp_port=56933 / ssh=musclotte.inria.fr");
		t.lookupService(ComponentDeployer.class).deploy(Set.of(server), true, 15, false, null, null);

//		t.lookupService(Deployer.class).deployLocalPeers(Set.of(remoteDB), true, ok -> s.add(ok));
//		Thing dbThing = s.iterator().next();
//		LMI.connect(t, dbThing);

		// loads the timeDB on the server - this one will really store figures
		t.lookupService(ServiceManager.class).start(TimeSeriesDB.class, server, 10);
		t.lookupService(RESTService.class).startHTTPServer();

		// creates the figure that will be fed
		client.createFigure("some metric", server);

		startGUI2(client, server);

		// runs the simulation
		for (int step = 0;; ++step) {
			// computes something
			Threads.sleepMs(100);
			System.out.println("sending point");
			// send point
			client.sendPoint("some metric", step, Math.random(), server, 1);
		}
	}

	private static void startGUI(TimeSeriesDBClient localDB, ComponentInfo remoteDB) {
		String parser = XMLResourceDescriptor.getXMLParserClassName();
		SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(parser);
		JSVGCanvas c = new JSVGCanvas();
		JFrame frame = Utilities.displayInJFrame(c, "demo for Julien");
		frame.setSize(800, 600);
		long startDate = System.currentTimeMillis();
		AtomicInteger i = new AtomicInteger();

		Threads.newThread_loop(() -> {

			try {
				String svg = new String(localDB.getPlot(Set.of("some metric"), "my first plot", "svg", remoteDB));
				SVGDocument document = factory.createSVGDocument(null, new StringReader(svg));
				c.setSVGDocument(document);
			} catch (Exception e) {
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

	private static void startGUI2(TimeSeriesDBClient client, ComponentInfo server) {
		JLabel c = new JLabel();
		JFrame frame = Utilities.displayInJFrame(c, "demo for Julien");
		frame.setSize(800, 600);

		long startDate = System.currentTimeMillis();
		AtomicInteger i = new AtomicInteger();

		Threads.newThread_loop(() -> {
			try {
				byte[] png = client.getPlot(Set.of("some metric"), "my first plot", "png", server);

				if (png != null) {
					c.setIcon(new ImageIcon(png));

					double durationS = ((System.currentTimeMillis() - startDate) / 1000);

					if (durationS > 0) {
						double freq = i.get() / durationS;
						System.out.println("updated " + freq + "frame/s");
					}

				}
				i.incrementAndGet();
			}
			catch (MessageException e) {
				e.printStackTrace();
			}
		});
	}
}
