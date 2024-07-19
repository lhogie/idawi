package idawi.service.publish_subscribe;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import idawi.Component;
import idawi.FunctionEndPoint;
import idawi.ProcedureEndpoint;
import idawi.Service;
import idawi.SupplierEndPoint;
import idawi.routing.QueueAddress;
import idawi.service.publish_subscribe.PublishSubscribeService.Publish.P;
import toools.SizeOf;

public class PublishSubscribeService extends Service {
	public static class Publication implements Serializable, SizeOf {
		public long ID = ThreadLocalRandom.current().nextLong();
		public String topic;
		public Object content;
		public double date;
		public long ref;

		@Override
		public String toString() {
			return "publication on " + topic + ": " + content;
		}

		@Override
		public long sizeOf() {
			return 8 + SizeOf.sizeOf(topic) + SizeOf.sizeOf(content) + 8 + 8;
		}
	}

	private final Map<String, Set<QueueAddress>> topic_subscribers = new HashMap<>();
	public final Map<String, List<Publication>> topic_history = new HashMap<>();

	public PublishSubscribeService(Component peer) {
		super(peer);
	}

	@Override
	public long sizeOf() {
		return super.sizeOf() + SizeOf.sizeOf(topic_history) + SizeOf.sizeOf(topic_history);
	}

	@Override
	public String getFriendlyName() {
		return "publish/subscribe";
	}

	public static class I {
		String topic;
		QueueAddress subscriber;
	}

	public class subscribe extends ProcedureEndpoint<I> {

		@Override
		public void doIt(I i) throws Throwable {
			ensureTopicExists(i.topic);
			topic_subscribers.get(i.topic).add(i.subscriber);
		}

		@Override
		public String getDescription() {
			return "adds a subscribtion";
		}
	}

	public class unsubscribe extends ProcedureEndpoint<I> {
		@Override
		public void doIt(I i) throws Throwable {
			ensureTopicExists(i.topic);
			topic_subscribers.get(i.topic).remove(i.subscriber);
		}

		@Override
		public String getDescription() {
			// TODO Auto-generated method stub
			return null;
		}
	}

	public class ListTopics extends SupplierEndPoint<List<String>> {
		@Override
		public List<String> get() {
			return new ArrayList(topic_history.keySet());
		}

		@Override
		public String getDescription() {
			return "topics known by this node";
		}
	}

	public class ListSubscribers extends FunctionEndPoint<String, Map<String, Set<QueueAddress>>> {
		@Override
		public Map<String, Set<QueueAddress>> f(String topic) throws Throwable {
			return topic_subscribers;
		}

		@Override
		public String getDescription() {
			return "list all subscribtions";
		}
	}

	public class Publish extends ProcedureEndpoint<P> {
		public static class P {
			String topic;
			Object content;
		}

		@Override
		public void doIt(P i) throws Throwable {
			publish(i.content, i.topic);
		}

		@Override
		public String getDescription() {
			return "do a new publication";
		}
	}

	public class LookupPublication extends FunctionEndPoint<Long, Publication> {
		@Override
		public Publication f(Long ID) {
			return lookup(ID);
		}

		@Override
		public String getDescription() {
			return "look up the publication with the given ID";
		}
	}

	public Publication lookup(long ID) {
		for (var history : topic_history.values()) {
			for (var publication : history) {
				if (publication.ID == ID) {
					return publication;
				}
			}
		}

		return null;
	}

	public void publish(Object o, String topic) {
		ensureTopicExists(topic);
		Publication publication = new Publication();
		publication.content = o;
		publication.topic = topic;
		publication.date = component.now();

		// store publication
		topic_history.get(topic).add(publication);

		// notify subscribers
		topic_subscribers.get(topic).forEach(subscriber -> send(publication, subscriber));
	}

	private void ensureTopicExists(String topic) {
		if (!topic_subscribers.containsKey(topic)) {
			topic_subscribers.put(topic, new HashSet<>());
			topic_history.put(topic, new ArrayList<>());
		}
	}

}
