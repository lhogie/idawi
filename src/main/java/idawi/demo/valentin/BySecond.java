package idawi.demo.valentin;

import it.unimi.dsi.fastutil.longs.Long2DoubleAVLTreeMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import toools.util.Date;

public class BySecond {
	// stores the number of messages received at each second
	public final Long2DoubleMap second2nbMessages = new Long2DoubleAVLTreeMap();
	private long lastSec;

	public BySecond() {
		second2nbMessages.defaultReturnValue(-1);
	}

	public void addSomeMore(double howMuch, double time) {
		long sec = (long) time;
		lastSec = Math.max(lastSec, sec);
		second2nbMessages.put(sec, second2nbMessages.get(sec) + howMuch);
	}

	public double howMuchAt(long sec) {
		return second2nbMessages.get(sec);
	}

	public double howMuch() {
		return second2nbMessages.get(lastSec);
	}

	public double howMuchInThePast(long rewindSecs) {
		return second2nbMessages.get(lastSec - rewindSecs);
	}

	public long lastSecond() {
		return lastSec;
	}

	public long removeOlderThan(long sec) {
		final long start = sec;
		while (second2nbMessages.remove(sec--) != -1)
			;
		return start - sec;
	}
	public LongSet secs() {
		return second2nbMessages.keySet(); 
	}
	
	public static void main(String[] args) {
		var b = new BySecond();
		
		while (true) {
			b.addSomeMore(1, Date.time());
			System.out.println(b.secs());
			System.out.println(b.howMuch());

			if (b.secs().size() > 5) {
				System.out.println("shrink to " + b.removeOlderThan(b.lastSecond() - 1));
			}
		}
	}
}
