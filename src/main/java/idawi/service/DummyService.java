package idawi.service;

import java.io.Serializable;
import java.util.Random;
import java.util.Set;

import idawi.AsMethodOperation.OperationID;
import idawi.Component;
import idawi.ComponentAddress;
import idawi.EOT;
import idawi.IdawiOperation;
import idawi.MessageQueue;
import idawi.ProgressRatio;
import idawi.RunningOperation;
import idawi.Service;
import idawi.net.LMI;
import toools.math.MathsUtilities;
import toools.thread.Threads;

public class DummyService extends Service {
	private String dummyData = "some fake data hold by the dummy service";

	public DummyService(Component component) {
		super(component);
	}

	public static OperationID waiting;

	@IdawiOperation
	public double waiting(double maxSeconds) {
		double seconds = MathsUtilities.pickRandomBetween(0, maxSeconds, new Random());
		Threads.sleepMs((long) (seconds * 1000));
		return seconds;
	}

	public static OperationID grep;

	@IdawiOperation
	public void grep(MessageQueue in) {
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

	public static void main(String[] args) {
		Component a = new Component();
		Component b = new Component();
		LMI.connect(a, b);

		Service s = new Service(a);
		var to = new ComponentAddress(Set.of(b.descriptor()));
		RunningOperation stub = s.exec(to, DummyService.stringLength, true, DummyService.grep);

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

	public static OperationID stringLength;

	@IdawiOperation
	public void stringLength(MessageQueue in) {
		var msg = in.get_non_blocking();
		String s = (String) msg.content;
		send(s.length(), msg.requester);
	}

	public static OperationID countFrom1toN;

	@IdawiOperation
	public void countFrom1toN(MessageQueue in) {
		var m = in.get_blocking();

		for (int i = 0; i < (Integer) m.content; ++i) {
			reply(m, i);
		}
	}

	public static class Range implements Serializable {
		public Range(int i, int j) {
			this.a = i;
			this.b = j;
		}

		int a, b;
	}

	public static OperationID countFromAtoB;

	@IdawiOperation
	public void countFromAtoB(MessageQueue in) {
		var m = in.get_blocking();
		var p = (Range) m.content;

		for (int i = p.a; i < p.b; ++i) {
			reply(m, i);
		}
	}

	public static OperationID throwError;

	@IdawiOperation
	public void throwError(MessageQueue in) throws Throwable {
		throw new Error("this is a test error");
	}

	@IdawiOperation
	public void sendProgressInformation(MessageQueue in) throws Throwable {
		var msg = in.get_blocking();
		int target = (Integer) msg.content;

		for (int i = 0; i < target; ++i) {
			reply(msg, i);
			reply(msg, new ProgressRatio(target, i));
		}
	}

}
