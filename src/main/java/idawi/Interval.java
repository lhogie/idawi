package idawi;

class Interval {
	int start, end;
	Interval next;

	public boolean contains(int pos) {
		return start <= pos && pos < end;
	};
}