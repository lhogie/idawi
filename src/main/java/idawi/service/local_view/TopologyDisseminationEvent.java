package idawi.service.local_view;

import idawi.Event;
import idawi.PointInTime;
import idawi.messaging.RoutingStrategy;
import idawi.routing.BallBroadcasting;
import idawi.routing.BallBroadcastingParms;
import idawi.routing.ComponentMatcher;
import idawi.service.local_view.LocalViewService.acceptHello;
import idawi.transport.Loopback;

class TopologyDisseminationEvent extends Event<PointInTime> {

	private final LocalViewService localView;

	public TopologyDisseminationEvent(double w, LocalViewService lv) {
		super(new PointInTime(w));
		this.localView = lv;
	}

	@Override
	public void run() {
		localView.exec(ComponentMatcher.all, LocalViewService.class,
				acceptHello.class, msg -> {
					msg.routingStrategy = new RoutingStrategy(localView.component.need(BallBroadcasting.class));
					msg.content = localView.helloMessage();
					msg.routingStrategy.parms.acceptTransport = t -> !(t instanceof Loopback);

					if (msg.routingStrategy.parms instanceof BallBroadcastingParms bbp) {
						bbp.energy = 10;
					}
				});

		localView.scheduleNextDisseminationMessage();
	}
};