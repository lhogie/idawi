package idawi.service.julien;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class PlotRequest implements Serializable {
	Set<String> figureNames = new HashSet<>();
	String title;
	String format;
}