package idawi.service.julien;

import org.jfree.graphics2d.svg.SVGGraphics2D;

import xycharter.Plot;

public class SVGPlotter {
	public static String plot(Plot p) {
		SVGGraphics2D g = new SVGGraphics2D(300, 200);
		g.setClip(0, 0, 800, 800);
		p.draw(g);
		return g.getSVGDocument();
	}
}
