package idawi;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import toools.util.Date;

public class MessageCollector {
	public double endDate, startDate;
	public double timeout;
	public final MessageList messages = new MessageList();
	public boolean stop;
	public final Set<ComponentDescriptor> blacklist = new HashSet<>();
	public boolean deliverProgress = true;
	public boolean deliverEOT = true;
	public boolean deliverError = true;
	private final MessageQueue q;

	public static double DEFAULT_COLLECT_DURATION = 60;

	public MessageCollector(MessageQueue q) {
		this.q = q;
	}

	public double remains() {
		return endDate - Date.time();
	}

	public double duration() {
		return Date.time() - startDate;
	}

	public void collect(final double initialCollectDuration, final double initialTimeout, Consumer<MessageCollector> userCode) {
		this.startDate = Date.time();
		this.endDate = startDate + initialCollectDuration;
		this.timeout = initialTimeout;

		while (remains() > 0 && !stop) {
			var msg = q.poll_sync(Math.min(remains(), initialTimeout));

			if (msg != null && !blacklist.contains(msg.route.source().component)) {
				if (msg.isProgress()) {
					if (deliverProgress) {
						messages.add(msg);
						userCode.accept(this);
					}
				} else if (msg.isEOT()) {
					if (deliverEOT) {
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
	}
}
