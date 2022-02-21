package idawi;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import toools.util.Date;

public class MessageCollector {
	public double endDate;
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

	public MessageCollector collect(double duration, double timeout, Consumer<MessageCollector> returnsHandler) {
		this.endDate = Date.time() + duration;
		this.timeout = timeout;

		while (true) {
			if (remains() <= 0) { // expired!
				q.delete();
				return this;
			}

			var m = q.get_blocking(Math.min(remains(), timeout));

			if (m != null && !blacklist.contains(m.route.source().component)) {
				if (m.isProgress() && deliverProgress) {
					messages.add(m);
					returnsHandler.accept(this);
				} else if (m.isEOT() && deliverEOT) {
					messages.add(m);
					returnsHandler.accept(this);
				} else if (m.isError() && deliverError) {
					messages.add(m);
					returnsHandler.accept(this);
				}else {
					messages.add(m);
					returnsHandler.accept(this);
				}

				if (stop) {
					q.delete();
					return this;
				}
			}
		}
	}
}
