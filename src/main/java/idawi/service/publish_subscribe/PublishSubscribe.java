package idawi.service.publish_subscribe;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import idawi.Component;
import idawi.ComponentDescriptor;
import idawi.Service;
import idawi.QueueAddress;

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

	public static class Subscription implements Serializable {
		public QueueAddress to;
		public String topic;

		@Override
		public String toString() {
			return "subscription for " + to.notYetReachedExplicitRecipients + " on topic " + topic;
		}
	}

	public static final String subscribe = "subscribe";
	public static final String unsubscribe = "unsubscribe";
	public static final String ls_topics = "ls_topics";
	public static final String ls_history = "ls_history";
	public static final String ls_subscribers = "ls_subscribers";
	public static final String publish = "publish";

	private final Map<String, Set<Subscription>> topic_subscribers = new HashMap<>();
	public final Map<String, List<Publication>> topic_history = new HashMap<>();

	public PublishSubscribe(Component peer) {
		super(peer);

		// some node wants to subscribe
		registerOperation(subscribe, (msg, returns) -> {
			Subscription s = (Subscription) msg.content;
			ensureTopicExists(s.topic);
			topic_subscribers.get(s.topic).add(s);
		});

		registerOperation(unsubscribe,
				(msg, returns) -> topic_subscribers.get((String) msg.content).remove(msg.route.source()));

		registerOperation(ls_topics, (msg, out) -> topic_subscribers.keySet());

		registerOperation(ls_history, (msg, out) -> topic_history.get((String) msg.content));

		registerOperation(ls_subscribers, (msg, out) -> topic_subscribers.get((String) msg.content));

		registerOperation(publish, (msg, returns) -> {
			Publication p = (Publication) msg.content;
			publish(p.content, p.topic);
		});
	}

	public void publish(Object o, String topic) {
		ensureTopicExists(topic);
		Publication p = new Publication();
		p.content = o;
		p.topic = topic;
		topic_history.get(topic).add(p);
		topic_subscribers.get(topic).forEach(s -> {
			send(p, s.to);
		});
	}

	private void ensureTopicExists(String topic) {
		if (!topic_subscribers.containsKey(topic)) {
			topic_subscribers.put(topic, new HashSet<>());
			topic_history.put(topic, new ArrayList<>());
		}
	}

	public static void subscribe(Service localService, ComponentDescriptor newPeer, Subscription subscription) {
		QueueAddress to = new QueueAddress();
		to.notYetReachedExplicitRecipients = Set.of(newPeer);
		to.service = PublishSubscribe.class;
		to.queue = PublishSubscribe.subscribe;
		localService.send(subscription, to);
	}

}
