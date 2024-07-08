package idawi.service.cloud;

class Meeting {
	final double startDate;
	double endDate;
	int connectionSpeed;

	public Meeting(double startTime) {
		this.startDate = startTime;
	}

	double duration() {
		return endDate - startDate;
	}

}