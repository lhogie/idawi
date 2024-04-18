package idawi.service;

import java.awt.Dimension;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import idawi.Component;
import idawi.FunctionEndPoint;
import idawi.InnerClassEndpoint;
import idawi.ProcedureEndpoint;
import idawi.Service;
import idawi.SupplierEndPoint;
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

	public class multipleRandomMessages extends InnerClassEndpoint<Integer, Integer> {

		@Override
		public String getDescription() {
			return "sends a random message every second";
		}

		@Override
		public void impl(MessageQueue in) throws Throwable {
			var tg = in.poll_sync();
			var n = parms(tg);

			for (int i = 0; i < n; ++i) {
				var eot = i == n - 1;
				sendd(new Random().nextInt(), tg.replyTo, msg -> msg.eot = eot);
				Threads.sleepMs(1000);
			}
		}
	}

	public class waiting extends ProcedureEndpoint<Double> {
		@Override
		public void doIt(Double d) {
			Threads.sleepMs((long) (d * 1000));
		}

		@Override
		public String getDescription() {
			return "just waits";
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

			sendd(null, trigger.replyTo, m -> m.eot = true);
		}

		@Override
		public String getDescription() {
			return null;
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
		public String getDescription() {
			return null;
		}

		@Override
		public Integer f(String s) throws Throwable {
			return s.length();
		}
	}

	public class chart extends SupplierEndPoint<Chart> {
		@Override
		public String r() {
			return "gives a chart";
		}

		@Override
		public Chart get() {
			return new Chart();
		}
	}

	public class countFrom1toN extends InnerClassEndpoint<AAA, Integer> {
		@Override
		public String getDescription() {
			return "replies n messages";
		}

		public static class AAA implements Serializable {
			public double sleepTime;
			public int n;
		}

		@Override
		public void impl(MessageQueue in) throws Throwable {
			var m = in.poll_sync();
			AAA a = parms(m);

			for (int i = 0; i < a.n; ++i) {
				var eot = i == a.n - 1;
				sendd(i, m.replyTo, msg -> msg.eot = eot);
				Threads.sleep(a.sleepTime);
			}
		}
	}

	public class countFromAtoB extends InnerClassEndpoint<Range, Integer> {
		@Override
		public String getDescription() {
			return null;
		}

		public static class Range implements Serializable, SizeOf {
			public Range(int i, int j) {
				this.a = i;
				this.b = j;
			}

			int a, b;

			@Override
			public long sizeOf() {
				return 8;
			}
		}

		@Override
		public void impl(MessageQueue in) {
			var m = in.poll_sync();
			var p = parms(m);

			for (int i = p.a; i < p.b; ++i) {
				var eot = i == p.b - 1;
				sendd(i, m.replyTo, msg -> msg.eot = eot);
			}
		}
	}

	public class loremPicsum extends FunctionEndPoint<Dimension, byte[]> {
		@Override
		public String getDescription() {
			return "returns a random image";
		}

		@Override
		public byte[] f(Dimension d) throws IOException {
			int id = new Random().nextInt(100);
			String url = "https://picsum.photos/id/" + id + "/" + d.width + "/" + d.height;
			return NetUtilities.retrieveURLContent(url);
		}
	}

	public class throwError extends InnerClassEndpoint {
		@Override
		public String getDescription() {
			return null;
		}

		@Override
		public void impl(MessageQueue in) {
			throw new Error("this is a test error");
		}
	}

	public class quitAll extends InnerClassEndpoint {
		@Override
		public String getDescription() {
			return null;
		}

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
				sendd(new ProgressRatio(i, target), msg.replyTo, m -> m.eot = eot);
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

			interface R extends Supplier<Object>, SizeOf {

				@Override
				public default long sizeOf() {
					return 0;
				}
			}

			List<R> l = new ArrayList<>();
			l.add(() -> new ProgressRatio(rand.nextInt(100), 100));
			l.add(() -> rand.nextInt());
			l.add(() -> {
				try {
					return lookupEndpoint(loremPicsum.class).f(new Dimension(200, 100));
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
			l.add(() -> new ProgressMessage("I'm still working!"));

			for (int i = 0; i < target; ++i) {
				if (in.size() > 0) {
					msg = in.poll_sync();

					if (msg.content.equals("stop")) {
						return;
					}
				}

				var d = l.get(rand.nextInt(l.size()));
				sendd(d.get(), msg.replyTo, m -> m.eot = false);
				Threads.sleep(rand.nextDouble());
			}

			sendd(new ProgressRatio(target, target), msg.replyTo, m -> m.eot = true);
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

			sendd(Graph.random(), msg.replyTo, m -> msg.eot = true);
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

			sendd(Chart.random(), msg.replyTo, m -> msg.eot = true);
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

			sendd(Image.random(), msg.replyTo, m -> msg.eot = true);
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
			var rd = new RawData();
			var bytes = NetUtilities.retrieveURLContent(
					"https://thumbs.static-thomann.de/thumb/padthumb600x600/pics/bdb/_43/439308/13826671_800.jpg");
			rd.base64 = TextUtilities.base64(bytes);
			rd.mimeType = "image/jpeg";

			sendd(rd, msg.replyTo, m -> msg.eot = true);
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

			sendd(Video.random(), msg.replyTo, m -> msg.eot = true);
		}
	}
}
