package idawi.service;

import java.util.Random;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

import idawi.Component;
import idawi.ExposedOperation;
import idawi.Message;
import idawi.OperationStandardForm;
import idawi.ProgressRatio;
import idawi.Service;
import toools.math.MathsUtilities;
import toools.thread.Threads;

public class DummyService extends Service {
	public DummyService(Component component) {
		super(component);
	}

	@ExposedOperation
	private double waiting(double maxSeconds) {
		double seconds = MathsUtilities.pickRandomBetween(0, maxSeconds, new Random());
		Threads.sleepMs((long) (seconds * 1000));
		return seconds;
	}

	@ExposedOperation
	private int stringLength(String s) {
		return s.length();
	}

	@ExposedOperation
	public Object nullOperation;

	@ExposedOperation
	public Runnable runnable = () -> {
	};

	@ExposedOperation
	public Callable callable = () -> "I'm a callable operation!";

	@ExposedOperation
	public Supplier supplier = () -> "I'm a supplier operation!";

	@ExposedOperation
	public Consumer<Message> msgConsumer = (msg) -> {
	};

	@ExposedOperation
	public BiConsumer<Message, Consumer> biConsumer = (msg, r) -> r.accept("I'm a biconsumer");

	@ExposedOperation
	public BiFunction<Message, Consumer, Object> biFunction = (m, r) -> "I'm a bifunction operation!";

	@ExposedOperation
	public OperationStandardForm fi = (m, r) -> r.accept("I'm a bifunction operation!");
	
	@ExposedOperation
	private void countFrom1toN(Message m, Consumer<Object> r) {
		for (int i = 0; i < (Integer) m.content; ++i) {
			r.accept(i);
		}
	}

	@ExposedOperation
	private void countFromAtoB(int a, int b, Consumer<Object> r) {
		for (int i = a; i < b; ++i) {
			r.accept(i);
		}
	}

	@ExposedOperation
	private void throwError() {
		throw new Error("this is a test error");
	}

	@ExposedOperation
	private void sendProgressInformation(Message m, Consumer<Object> r) {
		int target = (Integer) m.content;

		for (int i = 0; i < target; ++i) {
			r.accept(i);
			r.accept(new ProgressRatio(target, i));
		}
	}

}
