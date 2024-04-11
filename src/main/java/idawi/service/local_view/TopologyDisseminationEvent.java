package idawi.service.local_view;

import idawi.Event;
import idawi.PointInTime;
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
		localView.component.need(BallBroadcasting.class).exec(ComponentMatcher.all, LocalViewService.class,
				acceptHello.class, localView.helloMessage(), msg -> {
					msg.initialRoutingStrategy.parms.acceptTransport = t -> !(t instanceof Loopback);

					if (msg.initialRoutingStrategy.parms instanceof BallBroadcastingParms bbp) {
						bbp.energy = 10;
					}
				});

		localView.scheduleNextDisseminationMessage();
	}
};