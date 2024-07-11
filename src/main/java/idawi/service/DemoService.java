package idawi.service;

import java.awt.Dimension;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import javax.swing.JLabel;

import idawi.Component;
import idawi.Endpoint.EDescription;
import idawi.FunctionEndPoint;
import idawi.InnerClassEndpoint;
import idawi.ProcedureEndpoint;
import idawi.Service;
import idawi.SupplierEndPoint;
import idawi.messaging.Message;
import idawi.messaging.MessageQueue;
import idawi.messaging.ProgressMessage;
import idawi.messaging.ProgressRatio;
import idawi.service.DemoService.countFrom1toN.AAA;
import idawi.service.DemoService.countFromAtoB.Range;
import idawi.service.web.Graph;
import idawi.service.web.Image;
import idawi.service.web.RawData;
import idawi.service.web.Video;
import idawi.service.web.chart.Chart;
import toools.SizeOf;
import toools.io.Cout;
import toools.net.NetUtilities;
import toools.text.TextUtilities;
import toools.thread.Threads;

public class DemoService extends Service {
	private String dummyData = "some fake data hold by the dummy service";

	public DemoService(Component component) {
		super(component);
		registerEndpoint("e", q -> {
		});
	}

	@EDescription("sends a random message every second")
	public class multipleRandomMessages extends InnerClassEndpoint<Integer, Integer> {

		@Override
		public void impl(MessageQueue in) throws Throwable {
			var tg = in.poll_sync();
			var n = getInputFrom(tg);

			for (int i = 0; i < n; ++i) {
				var eot = i == n - 1;
				send(new Random().nextInt(), tg.replyTo, msg -> msg.eot = eot);
				Threads.sleepMs(1000);
			}
		}
	}

	@EDescription("waits")
	public class waiting extends ProcedureEndpoint<Double> {
		@Override
		public void doIt(Double d) {
			Threads.sleepMs((long) (d * 1000));
		}
	}

	public class grep extends InnerClassEndpoint<String, Integer> {
		@Override
		public void impl(MessageQueue in) throws Throwable {
			var trigger = in.poll_async();
			String re = (String) trigger.content;

			while (true) {
				var msg = in.poll_async();

				if (msg.isEOT()) {
					break;
				}

				String line = (String) msg.content;

				if (line.matches(re)) {
					send(line, trigger.replyTo);
				}
			}

			send(null, trigger.replyTo, m -> m.eot = true);
		}
	}

//	public static interface stringLength extends Operation2 {
//		public static String description = "compute length";
//
//		public static class frontEnd extends FrontEnd {
//			public int f(String s) {
//				MessageQueue future = from.send(s, new To(target, DummyService.stringLength.class));
//				return (Character) future.collect().throwAnyError_Runtime().get(0).content;
//			}
//		}
//
//		public static class backEnd extends Backend<DummyService> {
//			@Override
//			public void f(MessageQueue in) {
//				var msg = in.get_non_blocking();
//				String s = (String) msg.content;
//				service.send(s.length(), msg.replyTo);
//			}
//		}
//	}

	public class stringLength extends FunctionEndPoint<String, Integer> {
		@Override
		public Integer f(String s) throws Throwable {
			return s.length();
		}
	}

	@EDescription("gives a chart")
	public class chart extends SupplierEndPoint<Chart> {
		@Override
		public Chart get() {
			return new Chart();
		}
	}

	@EDescription("replies n messages")
	public class countFrom1toN extends InnerClassEndpoint<AAA, Integer> {

		public static class AAA implements Serializable {
			public double sleepTime;
			public int n;
		}

		@Override
		public void impl(MessageQueue in) throws Throwable {
			var m = in.poll_sync();
			AAA a = getInputFrom(m);

			for (int i = 0; i < a.n; ++i) {
				var eot = i == a.n - 1;
				send(i, m.replyTo, msg -> msg.eot = eot);
				Threads.sleep(a.sleepTime);
			}
		}
	}

	public class countFromAtoB extends InnerClassEndpoint<Range, Integer> {
		public static class Range implements Serializable, SizeOf {
			int a, b;

			public Range(int i, int j) {
				this.a = i;
				this.b = j;
			}

			@Override
			public long sizeOf() {
				return 8;
			}
		}

		@Override
		public void impl(MessageQueue in) {
			var m = in.poll_sync();
			var p = getInputFrom(m);

			for (int i = p.a; i < p.b; ++i) {
				var eot = i == p.b - 1;
				send(i, m.replyTo, msg -> msg.eot = eot);
			}
		}
	}

	@EDescription("random image")
	public class loremPicsum extends FunctionEndPoint<Dimension, byte[]> {

		@Override
		public byte[] f(Dimension d) throws IOException {
			return imageData(d);
		}

		public static byte[] imageData(Dimension d) throws IOException {
			int id = new Random().nextInt(100);
			String url = "https://picsum.photos/id/" + id + "/" + d.width + "/" + d.height;
			return NetUtilities.retrieveURLContent(url);
		}
	}

	public class throwError extends InnerClassEndpoint {

		@Override
		public void impl(MessageQueue in) {
			throw new Error("this is a test error");
		}
	}

	public class quitAll extends InnerClassEndpoint {

		@Override
		public void impl(MessageQueue in) {
			Cout.debugSuperVisible(in.poll_sync().route + ".System.exit(0)");
			System.exit(0);
		}
	}

	public class sendProgressInformation extends InnerClassEndpoint {
		@Override
		public String getDescription() {
			return null;
		}

		@Override
		public void impl(MessageQueue in) {
			var msg = in.poll_sync();
			int target = (Integer) msg.content;

			for (int i = 0; i < target; ++i) {
				send(i, msg.replyTo);
				var eot = i == target - 1;
				send(new ProgressRatio(i, target), msg.replyTo, m -> m.eot = eot);
			}
		}
	}

	public class complexResponse extends InnerClassEndpoint {
		@Override
		public String getDescription() {
			return "simulate a complex output";
		}

		@Override
		public void impl(MessageQueue in) throws IOException {
			var msg = in.poll_sync();
			int target = 100;

			var rand = new Random();

			List<Supplier<?>> suppliers = new ArrayList<>();
			suppliers.add(() -> new ProgressRatio(rand.nextInt(100), 100));
			suppliers.add(() -> new ProgressMessage("I'm still working!"));
			suppliers.add(() -> rand.nextInt());
			suppliers.add(() -> rand.nextBoolean());
			suppliers.add(() -> rand.nextDouble());
			suppliers.add(() -> rand.nextFloat());
			suppliers.add(() -> rand.nextLong());
			suppliers.add(() -> new JLabel("test"));
			suppliers.add(() -> TextUtilities.pickRandomString(rand, 1, 10));
			suppliers.add(() -> new Float[] { rand.nextFloat(), rand.nextFloat(), rand.nextFloat(), rand.nextFloat() });
			suppliers.add(
					() -> new Double[] { rand.nextDouble(), rand.nextDouble(), rand.nextDouble(), rand.nextDouble() });
			suppliers.add(() -> new Message<>());
			suppliers.add(() -> new IOException("some I/O error happened"));

			{
				var m = new HashMap<Integer, Double>();
				for (int i = 0; i < 10; ++i) {
					m.put(i, i / 2d);
				}

				suppliers.add(() -> m);
				suppliers.add(() -> new HashSet<>(m.keySet()));
				suppliers.add(() -> new HashSet<>(m.values()));
			}

			suppliers.add(() -> {
				try {
					return new RawData(loremPicsum.imageData(new Dimension(200, 100)), "image/jpeg");
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});

			suppliers.add(() -> {
				try {
					var array = new Object[2];
					array[0] = "coucou";
					array[1] = new RawData(loremPicsum.imageData(new Dimension(200, 100)), "image/jpeg");
					return array;
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});

			suppliers.add(() -> new RawData.javascript("alert( 'Hello, world!' );"));
			suppliers.add(() -> new RawData.csv("John,Paul,George,Ringo\nguitar,bass,guitar,drums"));
			suppliers.add(() -> new RawData.html("<a href=\"https://www.google.com/\">Hello <b>Google</b></a>"));

			suppliers.add(() -> {
				var svg = "<svg height=\"150\" width=\"500\" xmlns=\"http://www.w3.org/2000/svg\">\n"
						+ "  <ellipse cx=\"240\" cy=\"100\" rx=\"220\" ry=\"30\" fill=\"purple\" />\n"
						+ "  <ellipse cx=\"220\" cy=\"70\" rx=\"190\" ry=\"20\" fill=\"lime\" />\n"
						+ "  <ellipse cx=\"210\" cy=\"45\" rx=\"170\" ry=\"15\" fill=\"yellow\" />\n"
						+ "  Sorry, your browser does not support inline SVG. \n" + "</svg>";
				return new RawData(svg.getBytes(), "image/svg+xml");
			});

			for (int i = 0; i < target; ++i) {
				if (in.size() > 0) {
					msg = in.poll_sync();

					if (msg.content.equals("stop")) {
						return;
					}
				}

				var randomSupplier = suppliers.get(rand.nextInt(suppliers.size()));
				var randomValue = randomSupplier.get();
				send(randomValue, msg.replyTo, m -> m.eot = false);
				Threads.sleep(rand.nextDouble());
			}

			send(new ProgressRatio(target, target), msg.replyTo, m -> m.eot = true);
		}
	}

	public class SendGraph extends InnerClassEndpoint {
		@Override
		public String getDescription() {
			return "returns a simple graph";
		}

		@Override
		public void impl(MessageQueue in) throws IOException {
			var msg = in.poll_sync();
			send(Graph.random(), msg.replyTo, m -> msg.eot = true);
		}
	}

	public class SendChart extends InnerClassEndpoint {
		@Override
		public String getDescription() {
			return "returns a simple chart";
		}

		@Override
		public void impl(MessageQueue in) throws IOException {
			var msg = in.poll_sync();

			send(Chart.random(), msg.replyTo, m -> msg.eot = true);
		}
	}

	public class SendImage extends InnerClassEndpoint {
		@Override
		public String getDescription() {
			return "returns a simple image in base64";
		}

		@Override
		public void impl(MessageQueue in) throws IOException {
			var msg = in.poll_sync();

			send(Image.random(), msg.replyTo, m -> msg.eot = true);
		}
	}

	public class SendImageJPEG extends InnerClassEndpoint {
		@Override
		public String getDescription() {
			return "returns a simple image in JPEG";
		}

		@Override
		public void impl(MessageQueue in) throws IOException {
			var msg = in.poll_sync();
			var rd = new RawData(NetUtilities.retrieveURLContent(
					"https://thumbs.static-thomann.de/thumb/padthumb600x600/pics/bdb/_43/439308/13826671_800.jpg"),
					"image/jpeg");
			send(rd, msg.replyTo, m -> msg.eot = true);
		}
	}

	public class SendVideo extends InnerClassEndpoint {
		@Override
		public String getDescription() {
			return "returns a simple video in base64";
		}

		@Override
		public void impl(MessageQueue in) throws IOException {
			var msg = in.poll_sync();

			send(Video.random(), msg.replyTo, m -> msg.eot = true);
		}
	}
}
