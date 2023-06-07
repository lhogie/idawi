package idawi.service;

import java.io.IOException;
import java.io.Serializable;
import java.util.Random;

import idawi.Component;
import idawi.InnerClassOperation;
import idawi.OperationParameterList;
import idawi.Service;
import idawi.TypedInnerClassOperation;
import idawi.messaging.EOT;
import idawi.messaging.MessageQueue;
import idawi.messaging.ProgressMessage;
import idawi.messaging.ProgressRatio;
import idawi.routing.BlindBroadcasting;
import idawi.routing.ComponentMatcher;
import idawi.service.web.Graph;
import idawi.service.web.Image;
import idawi.service.web.Video;
import idawi.service.web.chart.Chart;
import idawi.transport.SharedMemoryTransport;
import toools.math.MathsUtilities;
import toools.net.NetUtilities;
import toools.thread.Threads;

public class DemoService extends Service {
	private String dummyData = "some fake data hold by the dummy service";

	public DemoService(Component component) {
		super(component);
		registerOperation(new countFrom1toN());
		registerOperation(new countFromAtoB());
		registerOperation(new grep());
		registerOperation(new sendProgressInformation());
		registerOperation(new stringLength());
		registerOperation(new chart());
		registerOperation(new throwError());
		registerOperation(new waiting());
		registerOperation(new multipleRandomMessages());
		registerOperation(new complexResponse());
		registerOperation(new SendGraph());
		registerOperation(new SendChart());
		registerOperation(new SendImage());
		registerOperation(new SendVideo());
		registerOperation("e", q -> {
		});

	}

	public class multipleRandomMessages extends InnerClassOperation {

		@Override
		public String getDescription() {
			return "sends a random message every second";
		}

		@Override
		public void impl(MessageQueue in) throws Throwable {
			var tg = in.poll_sync();
			var opl = (OperationParameterList) tg.content;
			int n = Integer.valueOf(opl.get(0).toString());

			for (int i = 0; i < n; ++i) {
				reply(tg, new Random().nextInt());
				Threads.sleepMs(1000);
			}
		}
	}

	public class waiting extends TypedInnerClassOperation {
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

	public class grep extends InnerClassOperation {
		@Override
		public void impl(MessageQueue in) throws Throwable {
			var trigger = in.poll_async();
			String re = (String) trigger.content;

			while (true) {
				var msg = in.poll_async();

				if (msg.content instanceof EOT) {
					break;
				}

				String line = (String) msg.content;

				if (line.matches(re)) {
					reply(trigger, line);
				}
			}
		}

		@Override
		public String getDescription() {
			return null;
		}

	}

	public static void main(String[] args) {
		Component a = new Component();
		Component b = new Component();
		a.lookup(SharedMemoryTransport.class).connectTo(b);

		var s = new BlindBroadcasting(a);
		var o = s.exec(DemoService.stringLength.class, null,  ComponentMatcher.one(b), true, "");

		for (int i = 0; i < 50; ++i) {
			s.send("" + i, o);
		}

		o.returnQ.c().collect(c -> c.messages.last().getClass());
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

	public class stringLength extends TypedInnerClassOperation {
		public int f(String s) {
			return s.length();
		}

		@Override
		public String getDescription() {
			return null;
		}
	}

	public class chart extends TypedInnerClassOperation {
		public Chart f() {
			return new Chart();
		}

		@Override
		public String getDescription() {
			return "gives a chart";
		}
	}

	public class countFrom1toN extends InnerClassOperation {
		@Override
		public String getDescription() {
			return "replies n messages";
		}

		@Override
		public void impl(MessageQueue in) throws Throwable {
			var m = in.poll_sync();

			for (int i = 0; i < (Integer) m.content; ++i) {
				reply(m, i);
			}
		}
	}

	public static class Range implements Serializable {
		public Range(int i, int j) {
			this.a = i;
			this.b = j;
		}

		int a, b;
	}

	public class countFromAtoB extends InnerClassOperation {
		@Override
		public String getDescription() {
			return null;
		}

		public void impl(MessageQueue in) {
			var m = in.poll_sync();
			var p = (Range) m.content;

			for (int i = p.a; i < p.b; ++i) {
				reply(m, i);
			}
		}
	}

	public class loremPicsum extends TypedInnerClassOperation {
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

	public class throwError extends InnerClassOperation {
		@Override
		public String getDescription() {
			return null;
		}

		@Override
		public void impl(MessageQueue in) {
			throw new Error("this is a test error");
		}
	}

	public class sendProgressInformation extends InnerClassOperation {
		@Override
		public String getDescription() {
			return null;
		}

		@Override
		public void impl(MessageQueue in) {
			var msg = in.poll_sync();
			int target = (Integer) msg.content;

			for (int i = 0; i < target; ++i) {
				reply(msg, i);
				reply(msg, new ProgressRatio(i, target));
			}
		}
	}

	public class complexResponse extends InnerClassOperation {
		@Override
		public String getDescription() {
			return "simulate a complex output";
		}

		@Override
		public void impl(MessageQueue in) throws IOException {
			var msg = in.poll_sync();
			int target = 100;

			var rand = new Random();

			for (int i = 0; i < target; ++i) {
				var d = rand.nextDouble();

				if (d < 0.1) {
					reply(msg, new ProgressRatio(i, target));
				} else if (d < 0.2) {
					reply(msg, rand.nextInt());
				} else if (d < 0.2) {
					reply(msg, new Error("test error"));
				} else if (d < 0.2) {
					reply(msg, lookupOperation(loremPicsum.class).f(200, 100));
				} else if (d < 0.3) {
					reply(msg, Chart.random());
				} else if (d < 0.4) {
					reply(msg, Graph.random());
				} else if (d < 0.4) {
					reply(msg, new ProgressMessage("I'm still working!"));
				} else {
				}

				Threads.sleep(rand.nextDouble());
			}

			reply(msg, new ProgressRatio(target, target));
		}
	}

	public class SendGraph extends InnerClassOperation {
		@Override
		public String getDescription() {
			return "returns a simple graph";
		}

		@Override
		public void impl(MessageQueue in) throws IOException {
			var msg = in.poll_sync();

			reply(msg, Graph.random());
		}
	}

	public class SendChart extends InnerClassOperation {
		@Override
		public String getDescription() {
			return "returns a simple chart";
		}

		@Override
		public void impl(MessageQueue in) throws IOException {
			var msg = in.poll_sync();

			reply(msg, Chart.random());
		}
	}

	public class SendImage extends InnerClassOperation {
		@Override
		public String getDescription() {
			return "returns a simple image in base64";
		}

		@Override
		public void impl(MessageQueue in) throws IOException {
			var msg = in.poll_sync();

			reply(msg, Image.random());
		}
	}

	public class SendVideo extends InnerClassOperation {
		@Override
		public String getDescription() {
			return "returns a simple video in base64";
		}

		@Override
		public void impl(MessageQueue in) throws IOException {
			var msg = in.poll_sync();

			reply(msg, Video.random());
		}
	}
}