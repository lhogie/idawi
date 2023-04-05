package idawi.service.web.chart;

import java.util.ArrayList;
import java.util.List;

public class Chart {
	System system;
	List<Function> functions = new ArrayList<>();

	public static Chart random() {
		Chart c = new Chart();

		{
			Function cos = new Function();
			Function sin = new Function();
			Function afin = new Function();

			for (double x = -1; x < 1; ++x) {
				{
					var p = new Point();
					p.x = x;
					p.y = Math.cos(x);
					cos.points.add(p);
				}
				{
					var p = new Point();
					p.x = x;
					p.y = Math.cos(x);
					sin.points.add(p);
				}
				{
					var p = new Point();
					p.x = x;
					p.y = 2 * x - 1;
					afin.points.add(p);
				}
			}
			
			c.functions.add(cos);
			c.functions.add(sin);
			c.functions.add(afin);
		}

		return c;
	}
}
