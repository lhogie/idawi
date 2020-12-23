package idawi;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import toools.io.Cout;
import toools.thread.Q;

public class MessageQueue extends Q<Message> {
	public final String name;
	private final Consumer<MessageQueue> destructor;
	private final Set<ComponentInfo> allowedSenders;
	private final Set<ComponentInfo> completedSenders = new HashSet<>();
	private double timeoutS = DEFAULT_TIMEOUT_IN_SECONDS;
	public static double DEFAULT_TIMEOUT_IN_SECONDS = 60;

	public enum SUFFICIENCY {
		ENOUGH, NOT_ENOUGH;
	}

	public MessageQueue(String name, Set<ComponentInfo> expectedSenders, int capacity,
			Consumer<MessageQueue> destructor) {
		super(capacity);
		this.destructor = destructor;
		this.allowedSenders = expectedSenders;
		this.name = name;
	}

	public MessageQueue setTimeout(double timeoutS) {
		this.timeoutS = timeoutS;
		return this;
	}

	// sends a message and waits for returns, until timeout, EOT or explicit req by
	// the returnsHandler
	public boolean forEach(Function<Message, SUFFICIENCY> returnsHandler) {
		while (true) {
			Message msg = get_blocking(timeoutS);

			if (msg == null) {
				destructor.accept(this);
				return false;
			} else if (msg.isEOT()) {
				completedSenders.add(msg.route.source().component);

				if (returnsHandler.apply(msg) == SUFFICIENCY.ENOUGH || completedSenders.equals(allowedSenders)) {
					destructor.accept(this);
					return true;
				}
			} else if (returnsHandler.apply(msg) == SUFFICIENCY.ENOUGH) {
				destructor.accept(this);
				return true;
			}
		}
	}

	public boolean forEach2(Consumer<Message> h) {
		return forEach(msg -> {
			h.accept(msg);
			return SUFFICIENCY.NOT_ENOUGH;
		});
	}

	// sends a message and waits for returns, until EOT or the returnsHandler asks
	// to stop waiting
	// incoming messages are demultiplxed according to their role
	public boolean forEach(Function<Message, SUFFICIENCY> resultHandler, Function<Message, SUFFICIENCY> errorHandler,
			Function<Message, SUFFICIENCY> progressHandler, Function<Message, SUFFICIENCY> completionHandler) {
		return forEach(r -> {
			if (r.isEOT()) {
				return completionHandler.apply(r);
			} else if (r.isError()) {
				return errorHandler.apply(r);
			} else if (r.isProgress()) {
				return progressHandler.apply(r);
			} else {
				return resultHandler.apply(r);
			}
		});
	}

	public Object get() throws Throwable {
		return collectUntilFirstEOT().throwAnyError().resultMessages().first().content;
	}

	/**
	 * Collects until all components have replied or timeout has expired.
	 * 
	 * @return
	 */
	public MessageList collect() {
		return collect(msg -> SUFFICIENCY.NOT_ENOUGH);
	}

	/**
	 * Collects until one component have completed.
	 * 
	 * @return
	 */
	public MessageList collectUntilFirstEOT() {
		return collect(msg -> msg.isEOT() ? SUFFICIENCY.ENOUGH : SUFFICIENCY.NOT_ENOUGH);
	}

	public MessageList collect(Function<Message, SUFFICIENCY> returnsHandler) {
		MessageList l = new MessageList();
		l.timeout = !forEach(msg -> {
			if (msg.content instanceof Throwable && !(msg.content instanceof RemoteException))
				Cout.debugSuperVisible(msg.content);

			l.add(msg);
			return returnsHandler.apply(msg);
		});

		return l;
	}
}
