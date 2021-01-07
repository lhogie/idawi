package idawi;

import java.io.Serializable;

import toools.util.Date;

public interface Descriptor extends Serializable {
	public double date = Date.time();

	public default boolean isNewerThan(Descriptor b) {
		return date > b.date;
	}
}
