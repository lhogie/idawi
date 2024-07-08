package idawi.service.web.chart;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

class Function implements Serializable {
	List<Point> points = new ArrayList<>();
	String color;
}