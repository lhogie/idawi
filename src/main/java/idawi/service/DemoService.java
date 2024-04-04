package idawi.service;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import idawi.Component;
import idawi.EndpointParameterList;
import idawi.InnerClassEndpoint;
import idawi.Service;
import idawi.TypedInnerClassEndpoint;
import idawi.messaging.MessageQueue;
import idawi.messaging.ProgressMessage;
import idawi.messaging.ProgressRatio;
import idawi.service.web.Graph;
import idawi.service.web.Image;
import idawi.service.web.Video;
import idawi.service.web.chart.Chart;
import toools.SizeOf;
import toools.io.Cout;
import toools.math.MathsUtilities;
import toools.net.NetUtilities;
import toools.thread.Threads;

public class DemoService extends Service {
	private String dummyData = "some fake data hold by the dummy service";

	public DemoService(Component component) {
		super(component);
		registerEndpoint("e", q -> {
		});
	}

	public class multipleRandomMessages extends InnerClassEndpoint {

		@Override
		public String getDescription() {
			return "sends a random message every second";
		}

		@Override
		public void impl(MessageQueue in) throws Throwable {
			var tg = in.poll_sync();
			var opl = (EndpointParameterList) tg.content;
			int n = Integer.valueOf(opl.get(0).toString());

			for (int i = 0; i < n; ++i) {
				reply(tg, new Random().nextInt(), i == n - 1);
				Threads.sleepMs(1000);
			}
		}
	}

	public class waiting extends TypedInnerClassEndpoint {
		public double waiting(double maxSeconds) {
			double seconds = MathsUtilities.pickRandomBetween(0, maxSeconds, new Random());
			Threads.sleepMs((long) (seconds * 1000));
			return seconds;
		}

		@Override
		public String getDescription() {
			// TODO Auto-generated method stub
			return null;
		}
	}

	public class grep extends InnerClassEndpoint {
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
					reply(trigger, line, false);
				}
			}

			reply(trigger, null, true);
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

	public class stringLength extends TypedInnerClassEndpoint {
		public int f(String s) {
			return s.length();
		}

		@Override
		public String getDescription() {
			return null;
		}
	}

	public class chart extends TypedInnerClassEndpoint {
		public Chart f() {
			return new Chart();
		}

		@Override
		public String getDescription() {
			return "gives a chart";
		}
	}

	public class countFrom1toN extends InnerClassEndpoint {
		@Override
		public String getDescription() {
			return "replies n messages";
		}

		@Override
		public void impl(MessageQueue in) throws Throwable {
			var m = in.poll_sync();
			var l = (EndpointParameterList) m.content;
			int n = Integer.valueOf((String) l.getFirst());

			for (int i = 0; i < n; ++i) {
				reply(m, i, i == n - 1);
				Threads.sleep(1);
			}
		}
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

	public class countFromAtoB extends InnerClassEndpoint {
		@Override
		public String getDescription() {
			return null;
		}

		public void impl(MessageQueue in) {
			var m = in.poll_sync();
			var p = (Range) m.content;

			for (int i = p.a; i < p.b; ++i) {
				reply(m, i, i == p.b - 1);
			}
		}
	}

	public class loremPicsum extends TypedInnerClassEndpoint {
		@Override
		public String getDescription() {
			return "returns a random image";
		}

		public byte[] f(int w, int h) throws IOException {
			int id = new Random().nextInt(100);
			String url = "https://picsum.photos/id/" + id + "/" + w + "/" + h;
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
				reply(msg, i, false);
				reply(msg, new ProgressRatio(i, target), i == target - 1);
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
					return ((loremPicsum) lookupEndpoint(loremPicsum.class.getSimpleName())).f(200, 100);
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
				reply(msg, d.get(), false);
				Threads.sleep(rand.nextDouble());
			}

			reply(msg, new ProgressRatio(target, target), true);
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

			reply(msg, Graph.random(), true);
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

			reply(msg, Chart.random(), true);
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

			reply(msg, Image.random(), true);
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

			reply(msg, Video.random(), true);
		}
	}
}
