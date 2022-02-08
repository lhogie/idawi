package idawi;

import java.io.Serializable;

import toools.util.Date;

public interface Descriptor extends Serializable {
	public double date = Date.time();
	public double validity = 1;
	public boolean valid = true;

	public default boolean isNewerThan(Descriptor b) {
		return date > b.date;
	}
	
	public default boolean isOutOfDate() {
		return Date.time() > date + validity;
	}
}
