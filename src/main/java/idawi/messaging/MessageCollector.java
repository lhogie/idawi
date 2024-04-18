package idawi.messaging;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import idawi.Component;
import idawi.Idawi;

public class MessageCollector {
	public double endDate, startDate;
	public final MessageList messages = new MessageList();
	public boolean gotEnough;
	private final MessageQueue q;
	public ProgressRatio lastProgressRation;
	public static double DEFAULT_COLLECT_DURATION = 3;

	public MessageCollector(MessageQueue q) {
		this.q = q;
	}

	public double remainingTime() {
		return endDate - Idawi.agenda.time();
	}

	public double duration() {
		return Idawi.agenda.time() - startDate;
	}

	public MessageCollector collect(double timeout, Consumer<MessageCollector> userCode) {
		this.startDate = Idawi.agenda.time();
		this.endDate = startDate + timeout;

		while (remainingTime() > 0 && !gotEnough) {
			var msg = q.poll_sync(remainingTime());

			if (msg != null) {
				if (msg.content instanceof ProgressRatio ratio) {
					lastProgressRation = ratio;
				}

				messages.add(msg);
				userCode.accept(this);
			}
		}

		q.detach();
		return this;
	}

	public double estimatedTimeRemaining() {
		if (lastProgressRation == null) {
			return -1;
		}

		double duration = Idawi.agenda.time() - startDate;
		return duration * 1 / lastProgressRation.ratio() - duration;
	}

	public List<Object> collectNResults(double timeout, int n) {
		collect(timeout, c -> c.gotEnough = c.messages.count(m -> m.isResult()) >= n);
		return messages.throwAnyError().resultMessages(n).stream().map(m -> m.content).toList();
	}

	public MessageCollector collectUntil(Predicate<MessageCollector> terminationCondition) {
		var timeout = MessageCollector.DEFAULT_COLLECT_DURATION;
		collect(timeout, c -> c.gotEnough = !terminationCondition.test(c));
		return this;
	}

	public MessageCollector collectDuring(double durationS) {
		return collectUntil(c -> c.duration() >= durationS);
	}

	public void collect(Consumer<MessageCollector> collector) {
		var d = MessageCollector.DEFAULT_COLLECT_DURATION;
		collect(d, collector);
	}

	public void collectUntilAllHaveReplied(final double initialDuration, Set<Component> expectedRepliers) {
		collect(initialDuration, c -> c.gotEnough = expectedRepliers.containsAll(c.messages.senders()));
	}

	public void collectUntilNEOT(double timeout, int n) {
		collect(timeout, c -> c.gotEnough = c.messages.countEOT() == n);
	}
}
