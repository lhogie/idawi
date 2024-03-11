package idawi.service.local_view;

import idawi.Event;
import idawi.PointInTime;
import idawi.service.local_view.LocalViewService.acceptHello;

class TopologyDisseminationEvent extends Event<PointInTime> {

	private final LocalViewService localView;

	public TopologyDisseminationEvent(double w, LocalViewService lv) {
		super(new PointInTime(w));
		this.localView = lv;
	}

	@Override
	public void run() {
		localView.routing().exec(LocalViewService.class, acceptHello.class, localView.helloMessage(), true);
		localView.scheduleNextDisseminationMessage();
	}
};