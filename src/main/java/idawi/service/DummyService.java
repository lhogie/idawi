package idawi.service;

import java.util.Random;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

import idawi.Component;
import idawi.IdawiExposed;
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

	@IdawiExposed
	private double waiting(double maxSeconds) {
		double seconds = MathsUtilities.pickRandomBetween(0, maxSeconds, new Random());
		Threads.sleepMs((long) (seconds * 1000));
		return seconds;
	}

	@IdawiExposed
	private int stringLength(String s) {
		return s.length();
	}

	@IdawiExposed
	public Object nullOperation;

	@IdawiExposed
	public Runnable runnable = () -> {
	};

	@IdawiExposed
	public Callable callable = () -> "I'm a callable operation!";

	@IdawiExposed
	public Supplier supplier = () -> "I'm a supplier operation!";

	@IdawiExposed
	public Consumer<Message> msgConsumer = (msg) -> {
	};

	@IdawiExposed
	public BiConsumer<Message, Consumer> biConsumer = (msg, r) -> r.accept("I'm a biconsumer");

	@IdawiExposed
	public BiFunction<Message, Consumer, Object> biFunction = (m, r) -> "I'm a bifunction operation!";

	@IdawiExposed
	public OperationStandardForm fi = (m, r) -> r.accept("I'm a bifunction operation!");
	
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
	private void throwError() {
		throw new Error("this is a test error");
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
