package idawi.net;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import idawi.ComponentDescriptor;
import idawi.Message;
import idawi.NeighborhoodListener;
import toools.io.Cout;
import toools.io.ser.JavaSerializer;
import toools.io.ser.Serializer;
import toools.util.Date;

public abstract class TransportLayer {
	public static Serializer serializer = new JavaSerializer();

	private Consumer<Message> messageConsumer;
	public final List<NeighborhoodListener> listeners = new ArrayList<>();

	protected void processIncomingMessage(Message msg) {
		msg.receptionDate = Date.time();
		messageConsumer.accept(msg);
	}

	protected abstract void start();

	protected abstract void stop();

	public abstract String getName();

	public abstract boolean canContact(ComponentDescriptor c);

	public abstract void injectLocalInfoTo(ComponentDescriptor c);

	public abstract Collection<ComponentDescriptor> neighbors();

	public abstract void send(Message msg, Collection<ComponentDescriptor> toNeighbors);

	public void setNewMessageConsumer(Consumer<Message> newConsumer) {
		if (newConsumer == this.messageConsumer)
			return;

		if (newConsumer == null) {
			if (this.messageConsumer != null) {
				stop();
			}

			this.messageConsumer = null;
		} else {
			final Consumer<Message> formerConsumer = this.messageConsumer;
			this.messageConsumer = newConsumer;

			if (formerConsumer == null) {
				start();
			}
		}
	}

	@Override
	public String toString() {
		return getName();
	}

	public static List<TransportLayer> findProtocolsWhichCanDealWith(ComponentDescriptor c,
			List<TransportLayer> protocols) {
		return protocols.stream().filter(p -> p.canContact(c)).collect(Collectors.toList());
	}

}
