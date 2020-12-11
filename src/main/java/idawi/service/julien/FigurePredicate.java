package idawi.service.julien;

import java.io.Serializable;

public interface FigurePredicate extends Serializable {
	boolean accept(String figureName);
}