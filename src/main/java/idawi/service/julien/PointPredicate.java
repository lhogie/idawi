package idawi.service.julien;

import java.io.Serializable;

public interface PointPredicate extends Serializable {
	boolean accept(double x, double y);
}