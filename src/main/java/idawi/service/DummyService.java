package idawi.service;

import java.util.Random;
import java.util.function.Consumer;

import idawi.BasicOperation;
import idawi.Component;
import idawi.EOT;
import idawi.FrontEnd;
import idawi.IdawiExposed;
import idawi.Message;
import idawi.MessageQueue;
import idawi.Operation2;
import idawi.OperationStandardForm;
import idawi.ParameterizedOperation;
import idawi.ProgressRatio;
import idawi.Service;
import idawi.To;
import idawi.net.LMI;
import toools.math.MathsUtilities;
import toools.thread.Threads;

public class DummyService extends Service {
	private String dummyData = "some fake data hold by the dummy service";

	public DummyService(Component component) {
		super(component);
	}

	@IdawiExposed
	private double waiting(double maxSeconds) {
		double seconds = MathsUtilities.pickRandomBetween(0, maxSeconds, new Random());
		Threads.sleepMs((long) (seconds * 1000));
		return seconds;
	}

	@IdawiExposed
	public static class grep implements Operation2 {
		public static String description = "unix-like grep";

		public static void backEnd(DummyService s, MessageQueue in) {
			String re = (String) in.get_non_blocking().content;

			while (true) {
				var msg = in.get_non_blocking();

				if (msg.content instanceof EOT) {
					break;
				}

				String line = (String) msg.content;

				if (line.matches(re)) {
					s.reply(msg, line);
				}
			}
		}
	}

	public static void main(String[] args) {
		Component a = new Component();
		Component b = new Component();
		LMI.connect(a, b);

		Service s = new Service(a);

		OperationStub stub = new OperationStub(s, new To(b.descriptor(), DummyService.grep.class));

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

	@IdawiExposed
	public class stringLength implements OperationStandardForm {
		@Override
		public void accept(MessageQueue in) {
			var msg = in.get_non_blocking();
			String s = (String) msg.content;
			send(s.length(), msg.replyTo);
		}
	}

	public static interface stringLengthSIgnature {
		int f(String s);
	}

	@IdawiExposed
	public static class stringLengthParameterized extends ParameterizedOperation<DummyService>
			implements stringLengthSIgnature {

		public static class frontEnd extends FrontEnd implements stringLengthSIgnature {
			@Override
			public int f(String s) {
				MessageQueue future = from.send(s, new To(target, DummyService.stringLengthParameterized.class));
				return (Character) future.collect().throwAnyError_Runtime().get(0).content;
			}
		}

		@Override
		public int f(String s) {
			service.dummyData.hashCode();
			return s.length();
		}
	}

	@IdawiExposed
	private void countFrom1toN(Message m, Consumer<Object> r) {
		for (int i = 0; i < (Integer) m.content; ++i) {
			r.accept(i);
		}
	}

	@IdawiExposed
	private void countFromAtoB(int a, int b, Consumer<Object> r) {
		for (int i = a; i < b; ++i) {
			r.accept(i);
		}
	}

	@IdawiExposed
	public class throwError {
		public void backEnd(MessageQueue in) throws Throwable {
			throw new Error("this is a test error");
		}
	}

	@IdawiExposed
	private void sendProgressInformation(Message m, Consumer<Object> r) {
		int target = (Integer) m.content;

		for (int i = 0; i < target; ++i) {
			r.accept(i);
			r.accept(new ProgressRatio(target, i));
		}
	}

}
