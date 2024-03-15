package idawi.messaging;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import idawi.Component;
import toools.util.Date;

public class MessageCollector {
	public double endDate, startDate;
	public double timeout;
	public final MessageList messages = new MessageList();
	public boolean stop;
	public final Set<Component> blacklist = new HashSet<>();
	public boolean deliverProgress = true;
	public boolean deliverError = true;
	private final MessageQueue q;
	public Object contentDescription;

	public static double DEFAULT_COLLECT_DURATION = 1;

	public MessageCollector(MessageQueue q) {
		this.q = q;
	}

	public double remainingTime() {
		return endDate - Date.time();
	}

	public double duration() {
		return Date.time() - startDate;
	}

	public MessageCollector collect(final double initialCollectDuration, final double initialTimeout,
			Consumer<MessageCollector> userCode) {
		this.startDate = Date.time();
		this.endDate = startDate + initialCollectDuration;
		this.timeout = initialTimeout;

		while (remainingTime() > 0 && !stop) {
			var msg = q.poll_sync(Math.min(remainingTime(), initialTimeout));

			if (msg != null && !msg.route.components().stream().anyMatch(c -> blacklist.contains(c))) {
				if (msg.isProgress()) {
					if (deliverProgress) {
						messages.add(msg);
						userCode.accept(this);
					}
				} else if (msg.isError()) {
					if (deliverError) {
						messages.add(msg);
						userCode.accept(this);
					}
				} else {
					messages.add(msg);
					userCode.accept(this);
				}
			}
		}

		q.detach();
		return this;
	}

	public Object collectOneResult(double timeout) {
		return collectNResults(timeout, 1).get(0);
	}

	public List<Object> collectNResults(double timeout, int n) {
		collect(timeout, timeout, c -> c.stop = c.messages.count(m -> m.isResult()) >= n);
		return messages.throwAnyError().resultMessages(n).stream().map(m -> m.content).toList();
	}

	public List<Object> collectNResults(int n) {
		return collectNResults(DEFAULT_COLLECT_DURATION, n);
	}

	public List<Object> collectNResults2(double timeout, int n) {
		return collect(timeout, c -> c.messages.count(m -> m.isResult()) > n,
				c -> c.messages.throwAnyError().resultMessages(n).contents());
	}

	public <R> List<R> collect(double timeout, Predicate<MessageCollector> p, Function<MessageCollector, List<R>> r) {
		collect(timeout, timeout, c -> c.stop = p.test(c));
		return r.apply(this);
	}

	public MessageCollector collectWhile(Predicate<MessageCollector> p) {
		var timeout = MessageCollector.DEFAULT_COLLECT_DURATION;
		collect(timeout, timeout, c -> c.stop = !p.test(c));
		return this;
	}

	public MessageCollector collectDuring(double durationS) {
		return collectWhile(c -> c.duration() < durationS);
	}

	public void collect(Consumer<MessageCollector> collector) {
		var d = MessageCollector.DEFAULT_COLLECT_DURATION;
		collect(d, d, collector);
	}

	public void collectUntilAllHaveReplied(final double initialDuration, Set<Component> expectedRepliers) {
		collect(initialDuration, initialDuration, c -> c.stop = expectedRepliers.containsAll(c.messages.senders()));
	}

	/**
	 * Collects until one component have completed.
	 * 
	 * @return
	 */
	public void collectUntilFirstEOT(double timeout) {
		collect(timeout, timeout, c -> c.stop = c.messages.last().isEOT());
	}

	public void collectUntilNEOT(double timeout, int n) {
		collect(timeout, timeout, c -> c.stop = c.messages.countEOT() == n);
	}
}
