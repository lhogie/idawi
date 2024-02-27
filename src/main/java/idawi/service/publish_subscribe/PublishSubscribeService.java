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
import idawi.Service;
import idawi.TypedInnerClassEndpoint;
import idawi.routing.Destination;
import idawi.routing.MessageQDestination;
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

	private final Map<String, Set<Destination>> topic_subscribers = new HashMap<>();
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

	public class subscribe extends TypedInnerClassEndpoint {
		public void exec(String topic, Destination subscriber) throws Throwable {
			ensureTopicExists(topic);
			topic_subscribers.get(topic).add(subscriber);
		}

		@Override
		public String getDescription() {
			return "adds a subscribtion";
		}
	}

	public class unsubscribe extends TypedInnerClassEndpoint {
		public void exec(String topic, MessageQDestination subscriber) throws Throwable {
			ensureTopicExists(topic);
			topic_subscribers.get(topic).remove(subscriber);
		}

		@Override
		public String getDescription() {
			// TODO Auto-generated method stub
			return null;
		}
	}

	public class ListTopics extends TypedInnerClassEndpoint {
		public List<String> exec() throws Throwable {
			return new ArrayList(topic_history.keySet());
		}

		@Override
		public String getDescription() {
			return "list topics known by this node";
		}
	}

	public class ListSubscribers extends TypedInnerClassEndpoint {
		public Map<String, Set<Destination>> exec(String topic) throws Throwable {
			return topic_subscribers;
		}

		@Override
		public String getDescription() {
			return "list all subscribtions";
		}
	}

	public class Publish extends TypedInnerClassEndpoint {
		public void exec(String topic, Object content) throws Throwable {
			publish(content, topic);
		}

		@Override
		public String getDescription() {
			return "do a new publication";
		}
	}

	public class LookupPublication extends TypedInnerClassEndpoint {
		public Publication exec(long ID) throws Throwable {
			return lookup(ID);
		}

		@Override
		public String getDescription() {
			return "look up the publication with the given ID";
		}
	}

	public Publication lookup(long ID) throws Throwable {
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
		topic_subscribers.get(topic)
				.forEach(subscriber -> component.defaultRoutingProtocol().send(o, true, subscriber));
	}

	private void ensureTopicExists(String topic) {
		if (!topic_subscribers.containsKey(topic)) {
			topic_subscribers.put(topic, new HashSet<>());
			topic_history.put(topic, new ArrayList<>());
		}
	}

}
