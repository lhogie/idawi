package idawi.ui;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import idawi.Component;
import idawi.ComponentInfo;
import idawi.Service;
import idawi.service.ComponentDeployer;
import idawi.service.ServiceManager;
import idawi.service.SystemMonitor;
import idawi.service.SystemMonitor.Info;
import idawi.service.publish_subscribe.PublishSubscribe;
import idawi.service.publish_subscribe.PublishSubscribe.Publication;
import idawi.service.publish_subscribe.PublishSubscribe.Subscription;
import toools.io.Cout;
import toools.thread.Threads;

public class system_monitor {
	public static void main(String[] args) throws Throwable {

		List<ComponentInfo> peers = ComponentInfo.fromPDL(Arrays.asList(args));

		Component localNode = new Component(ComponentInfo.fromCDL("name=system monitor"));
		Service localService = new Service(localNode) {
			@Override
			public String getFriendlyName() {
				return "system monitoring client";
			}
		};

		localService.registerOperation(null, (msg, returns) -> {
			Publication p = (Publication) msg.content;
			Info i = (Info) p.content;
			Cout.info(msg.route.source() + ": " + i.uptime.stdout.trim());
		});

		localNode.lookupService(ComponentDeployer.class).deploy(peers, true, 2, true, msg -> System.out.println(msg),
				newPeer -> {
					localNode.lookupService(ServiceManager.class).start(SystemMonitor.class, newPeer, 1);
					Subscription subscription = new Subscription();
					subscription.to.notYetReachedExplicitRecipients = Set.of(localNode.descriptor());
					subscription.to.operationOrQueue = null;
					subscription.to.service = localService.id;
					subscription.topic = "system monitor";
					PublishSubscribe.subscribe(localService, newPeer, subscription);
				});

		Threads.sleepForever();
	}
}
