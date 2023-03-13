package idawi.service.publish_subscribe;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import idawi.Component;
import idawi.Service;
import idawi.TypedInnerClassOperation;
import idawi.routing.MessageQDestination;

public class PublishSubscribe extends Service {
	public static class Publication implements Serializable {
		public String topic;
		public Object content;
		public double date;

		@Override
		public String toString() {
			return "publication on " + topic + ": " + content;
		}
	}

	@Override
	public String getFriendlyName() {
		return "publish/subscribe";
	}

	private final Map<String, Set<MessageQDestination>> topic_subscribers = new HashMap<>();
	public final Map<String, List<Publication>> topic_history = new HashMap<>();

	public PublishSubscribe(Component peer) {
		super(peer);

		registerOperation(new Publish());
		registerOperation(new ListSubscribers());
		registerOperation(new ListTopics());
		registerOperation(new subscribe());
		registerOperation(new unsubscribe());
	}

	public class subscribe extends TypedInnerClassOperation {
		public void exec(String topic, MessageQDestination subscriber) throws Throwable {
			ensureTopicExists(topic);
			topic_subscribers.get(topic).add(subscriber);
		}

		@Override
		public String getDescription() {
			// TODO Auto-generated method stub
			return null;
		}
	}

	public class unsubscribe extends TypedInnerClassOperation {
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

	public class ListTopics extends TypedInnerClassOperation {
		public List<String> exec() throws Throwable {
			return new ArrayList(topic_history.keySet());
		}

		@Override
		public String getDescription() {
			// TODO Auto-generated method stub
			return null;
		}
	}

	public class ListSubscribers extends TypedInnerClassOperation {
		public List<String> exec(String topic) throws Throwable {
			return new ArrayList(topic_subscribers.values());
		}

		@Override
		public String getDescription() {
			// TODO Auto-generated method stub
			return null;
		}
	}

	public class Publish extends TypedInnerClassOperation {
		public void exec(String topic, Object publication) throws Throwable {
			publish(publication, topic);
		}

		@Override
		public String getDescription() {
			// TODO Auto-generated method stub
			return null;
		}
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
		topic_subscribers.get(topic).forEach(subscriber -> component.bb().send(publication, subscriber));
	}

	private void ensureTopicExists(String topic) {
		if (!topic_subscribers.containsKey(topic)) {
			topic_subscribers.put(topic, new HashSet<>());
			topic_history.put(topic, new ArrayList<>());
		}
	}

}
