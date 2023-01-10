package idawi;

public class ProgressRatio extends ProgressInformation {
	long target, progress;

	public ProgressRatio(long progress, long target) {
		this.target = target;
		this.progress = progress;
	}

	public ProgressRatio() {
		this(0, 1);
	}

	public double ratio() {
		if (completed()) {
			return 1;
		} else {
			return progress / (double) target;
		}
	}

	public boolean completed() {
		return progress >= target;
	}

	@Override
	public String toString() {
		if (completed()) {
			return "completed " + target + "/" + target;
		} else {
			return (((int) (10000 * ratio())) / 100d) + "%";
		}
	}
}