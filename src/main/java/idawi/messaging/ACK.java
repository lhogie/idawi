package idawi.messaging;

import java.util.Set;

import idawi.Component;
import idawi.Service;

public class ACK {

	public long ID;

	public ACK(Message msg) {
		this.ID = msg.ID;
	}

	public static final Set<Class<? extends ACK>> all = Set.of(serviceNotAvailable.class, eventScheduled.class,
			processingStarts.class, processingCompleted.class);

	public static class serviceNotAvailable extends ACK {

		public Component component;
		public Class<? extends Service> service;

		public serviceNotAvailable(Message msg) {
			super(msg);

		}

		public serviceNotAvailable(Class<? extends Service> service, Component component, Message msg) {
			this(msg);
			this.service = service;
			this.component = component;
		}
	}

	public static class eventScheduled extends ACK {

		public eventScheduled(Message msg) {
			super(msg);

		}
	}

	public static class processingStarts extends ACK {

		public processingStarts(Message msg) {
			super(msg);

		}
	}

	public static class processingCompleted extends ACK {

		public processingCompleted(Message msg) {
			super(msg);

		}
	}
};