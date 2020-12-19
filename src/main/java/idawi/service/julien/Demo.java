package idawi.service.julien;

import java.io.IOException;
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

import idawi.CDLException;
import idawi.ComponentInfo;
import idawi.RemoteException;
import idawi.Service;
import idawi.service.ComponentDeployer;
import idawi.service.ServiceManager;
import idawi.service.rest.RESTService;
import toools.gui.Utilities;
import toools.thread.Threads;

public class Demo {

	public static void main(String[] args) throws CDLException, IOException, RemoteException {
		System.out.println("start");

		new Service() {
			@Override
			public void run() throws IOException, RemoteException {
				// start a new JVM to host the time series DB
				ComponentInfo serverDescriptor = ComponentInfo.fromCDL("name=db / udp_port=56933 / ssh=musclotte.inria.fr");
				var server = new TimeSeriesDBStub(this, Set.of(serverDescriptor));
				service(ComponentDeployer.class).deploy(Set.of(serverDescriptor), true, 15, false, null, null);
				new ServiceManager.Stub(this, Set.of(serverDescriptor)).start(TimeSeriesDB.class);
				service(RESTService.class).startHTTPServer();

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

	private static void startGUI(TimeSeriesDBStub localDB, ComponentInfo remoteDB) {
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

	private static void startGUI2(TimeSeriesDBStub client, ComponentInfo server) {
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
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		});
	}
}
