package idawi.service.cloud;

import java.util.ArrayList;
import java.util.stream.Collectors;

class MeetingHistory extends ArrayList<Meeting> {
	public double totalTime() {
		return lastSeen() - firstSeen();
	}

	public double totalMeetingTime() {
		return stream().map(t -> t.duration()).collect(Collectors.summingDouble(Double::doubleValue));
	}

	public double avgMeetingDuration() {
		return stream().map(t -> t.duration()).collect(Collectors.averagingDouble(Double::doubleValue));
	}

	public double firstSeen() {
		return firstMeeting().startDate;
	}

	public double lastSeen() {
		return lastMeeting().endDate;
	}

	public Meeting lastMeeting() {
		return get(size() - 1);
	}

	public Meeting firstMeeting() {
		return get(0);
	}

}