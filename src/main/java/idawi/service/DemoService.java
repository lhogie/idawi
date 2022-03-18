package idawi.service;

import java.io.Serializable;
import java.util.Random;
import java.util.Set;

import idawi.Component;
import idawi.EOT;
import idawi.InnerOperation;
import idawi.MessageQueue;
import idawi.OperationParameterList;
import idawi.ProgressRatio;
import idawi.RemotelyRunningOperation;
import idawi.Service;
import idawi.To;
import idawi.TypedInnerOperation;
import idawi.net.LMI;
import toools.math.MathsUtilities;
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
		registerOperation(new throwError());
		registerOperation(new waiting());
		registerOperation(new multipleRandomMessages());
		registerOperation("e", q -> {
		});

	}

	public class multipleRandomMessages extends InnerOperation {

		@Override
		public String getDescription() {
			return "sends a random message every second";
		}

		@Override
		public void exec(MessageQueue in) throws Throwable {
			var tg = in.get_blocking();
			var opl = (OperationParameterList) tg.content;
			int n = Integer.valueOf(opl.get(0).toString());

			for (int i = 0; i < n; ++i) {
				reply(tg, new Random().nextInt());
				Threads.sleepMs(1000);
			}
		}
	}

	public class waiting extends TypedInnerOperation {
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

	public class grep extends InnerOperation {
		@Override
		public void exec(MessageQueue in) throws Throwable {
			String re = (String) in.get_non_blocking().content;

			while (true) {
				var msg = in.get_non_blocking();

				if (msg.content instanceof EOT) {
					break;
				}

				String line = (String) msg.content;

				if (line.matches(re)) {
					reply(msg, line);
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
		LMI.connect(a, b);

		Service s = new Service(a);
		var to = new To(Set.of(b.descriptor())).o(DemoService.stringLength.class);
		RemotelyRunningOperation stub = s.exec(to, true, DemoService.grep.class);

		for (int i = 0; i < 50; ++i) {
			stub.send("" + i);
		}

		stub.dispose();

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

	public class stringLength extends TypedInnerOperation {
		public int f(String s) {
			return s.length();
		}

		@Override
		public String getDescription() {
			return null;
		}
	}

	public class countFrom1toN extends InnerOperation {
		@Override
		public String getDescription() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void exec(MessageQueue in) throws Throwable {
			var m = in.get_blocking();

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

	public class countFromAtoB extends InnerOperation {
		@Override
		public String getDescription() {
			return null;
		}

		public void exec(MessageQueue in) {
			var m = in.get_blocking();
			var p = (Range) m.content;

			for (int i = p.a; i < p.b; ++i) {
				reply(m, i);
			}
		}
	}

	public class throwError extends InnerOperation {
		@Override
		public String getDescription() {
			return null;
		}

		@Override
		public void exec(MessageQueue in) {
			throw new Error("this is a test error");
		}
	}

	public class sendProgressInformation extends InnerOperation {
		@Override
		public String getDescription() {
			return null;
		}

		@Override
		public void exec(MessageQueue in) {
			var msg = in.get_blocking();
			int target = (Integer) msg.content;

			for (int i = 0; i < target; ++i) {
				reply(msg, i);
				reply(msg, new ProgressRatio(target, i));
			}
		}
	}
}
