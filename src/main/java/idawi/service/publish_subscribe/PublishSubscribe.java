package idawi.service.publish_subscribe;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import idawi.Component;
import idawi.InnerClassTypedOperation;
import idawi.QueueAddress;
import idawi.Service;

public class PublishSubscribe extends Service {
	public static class Publication implements Serializable {
		public String topic;
		public Object content;
		public long date;

		@Override
		public String toString() {
			return "publication on " + topic + ": " + content;
		}
	}

	@Override
	public String getFriendlyName() {
		return "publish/subscribe";
	}

	private final Map<String, Set<QueueAddress>> topic_subscribers = new HashMap<>();
	public final Map<String, List<Publication>> topic_history = new HashMap<>();

	public PublishSubscribe(Component peer) {
		super(peer);

		registerOperation("publish", (msg, returns) -> {
			Publication p = (Publication) msg.content;

		});
	}

	public class subscribe extends InnerClassTypedOperation {
		public void exec(String topic, QueueAddress subscriber) throws Throwable {
			ensureTopicExists(topic);
			topic_subscribers.get(topic).add(subscriber);
		}

		@Override
		public String getDescription() {
			// TODO Auto-generated method stub
			return null;
		}
	}

	public class unsubscribe extends InnerClassTypedOperation {
		public void exec(String topic, QueueAddress subscriber) throws Throwable {
			ensureTopicExists(topic);
			topic_subscribers.get(topic).remove(subscriber);
		}

		@Override
		public String getDescription() {
			// TODO Auto-generated method stub
			return null;
		}
	}

	public class ListTopics extends InnerClassTypedOperation {
		public List<String> exec() throws Throwable {
			return new ArrayList(topic_history.keySet());
		}

		@Override
		public String getDescription() {
			// TODO Auto-generated method stub
			return null;
		}
	}

	public class ListSubscribers extends InnerClassTypedOperation {
		public List<String> exec(String topic) throws Throwable {
			return new ArrayList(topic_subscribers.values());
		}

		@Override
		public String getDescription() {
			// TODO Auto-generated method stub
			return null;
		}
	}

	public class Publish extends InnerClassTypedOperation {
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
		Publication p = new Publication();
		p.content = o;
		p.topic = topic;
		topic_history.get(topic).add(p);
		topic_subscribers.get(topic).forEach(s -> send(p, s));
	}

	private void ensureTopicExists(String topic) {
		if (!topic_subscribers.containsKey(topic)) {
			topic_subscribers.put(topic, new HashSet<>());
			topic_history.put(topic, new ArrayList<>());
		}
	}

}
