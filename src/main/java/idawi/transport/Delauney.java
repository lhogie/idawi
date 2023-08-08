package idawi.transport;
// Java program to illustrate Delaunay Triangulation
// algorithm
import java.util.*;

import idawi.Component;

// Driver Class
public class Delauney {

	private static final double EPSILON = 1e-12;

	// structure to store the edge of the triangulation
	static class Edge {
		int a, b;

		Edge(int a, int b)
		{
			this.a = a;
			this.b = b;
		}
	}

	// structure to store the point in 2D space
	public 	static class Point {
		public double x, y;
		public Component c;

		Point(double x, double y)
		{
			this.x = x;
			this.y = y;
		}
	}

	// function to calculate the cross product of two
	// vectors
	private static double crossProduct(Point A, Point B)
	{
		return A.x * B.y - A.y * B.x;
	}

	// function to check if the point P is inside the circle
	// defined by points A, B, and C
	private static boolean insideCircle(Point A, Point B,
										Point C, Point P)
	{
		double ax = A.x - P.x;
		double ay = A.y - P.y;
		double bx = B.x - P.x;
		double by = B.y - P.y;
		double cx = C.x - P.x;
		double cy = C.y - P.y;

		double a2 = ax * ax + ay * ay;
		double b2 = bx * bx + by * by;
		double c2 = cx * cx + cy * cy;

		return (ax * (by - cy) + bx * (cy - ay)
				+ cx * (ay - by))
			>= EPSILON;
	}

	// main function to perform Delaunay triangulation
	public static List<Edge> triangulate(Point[] points)
	{
		int n = points.length;
		List<Edge> edges = new ArrayList<>();

		// sorting the points by x-coordinate
		Point[] sorted = new Point[n];
		for (int i = 0; i < n; i++) {
			sorted[i] = points[i];
		}
		sorted = sortByX(sorted, 0, n - 1);

		// creating the lower hull
		for (int i = 0; i < n; i++) {
			while (edges.size() >= 2) {
				int j = edges.size() - 2;
				int k = edges.size() - 1;
				Point A = sorted[edges.get(j).a];
				Point B = sorted[edges.get(j).b];
				Point C = sorted[edges.get(k).b];

				if (crossProduct(
						new Point(B.x - A.x, B.y - A.y),
						new Point(C.x - B.x, C.y - B.y))
					> 0) {
					break;
				}

				edges.remove(edges.size() - 1);
			}
			edges.add(new Edge(edges.size(), i));
		}
		int lower = edges.size();
		// creating the upper hull
		for (int i = n - 2, t = lower + 1; i >= 0; i--) {
			while (edges.size() >= t) {
				int j = edges.size() - 2;
				int k = edges.size() - 1;
				Point A = sorted[edges.get(j).a];
				Point B = sorted[edges.get(j).b];
				Point C = sorted[edges.get(k).b];

				if (crossProduct(
						new Point(B.x - A.x, B.y - A.y),
						new Point(C.x - B.x, C.y - B.y))
					> 0) {
					break;
				}

				edges.remove(edges.size() - 1);
			}
			edges.add(new Edge(i, edges.size()));
		}

		// removing the duplicate edges from the hull
		edges.remove(edges.size() - 1);

		// creating the triangulation
		List<Edge> result = new ArrayList<>();
		for (int i = 0; i < edges.size(); i++) {
			int a = edges.get(i).a;
			int b = edges.get(i).b;
			Point A = sorted[a];
			Point B = sorted[b];
			boolean flag = true;

			for (int j = 0; j < n; j++) {
				if (j == a || j == b) {
					continue;
				}
				Point P = sorted[j];
				if (insideCircle(A, B, P,
								sorted[a + b >> 1])) {
					flag = false;
					break;
				}
			}
			if (flag) {
				result.add(new Edge(a, b));
			}
		}

		return result;
	}

	// function to sort the points by x-coordinate
	private static Point[] sortByX(Point[] points,
								int start, int end)
	{
		if (start >= end) {
			return points;
		}
		int pivot = partition(points, start, end);
		sortByX(points, start, pivot - 1);
		sortByX(points, pivot + 1, end);
		return points;
	}

	// function to partition the points for quick sort
	private static int partition(Point[] points, int start,
								int end)
	{
		Point pivot = points[end];
		int i = start - 1;
		for (int j = start; j <= end - 1; j++) {
			if (points[j].x <= pivot.x) {
				i++;
				Point temp = points[i];
				points[i] = points[j];
				points[j] = temp;
			}
		}
		Point temp = points[i + 1];
		points[i + 1] = points[end];
		points[end] = temp;
		return i + 1;
	}

	// Main function
	public static void main(String[] args)
	{
		// Using Scanner for input
		Scanner sc = new Scanner(System.in);
		System.out.println("Enter the no. of coordinates:");

		int x, y, n = 0;
		n = sc.nextInt();

		// array created
		Point[] points = new Point[n];

		// Input coordinates
		for (int i = 0; i < n; i++) {
			System.out.println("Enter Coordinate No."
							+ " " + (i + 1));
			x = sc.nextInt();
			y = sc.nextInt();
			points[i] = new Point(x, y);
		}

		// List declared
		List<Edge> edges = triangulate(points);
		System.out.println("Triangulated Edges:");

		for (Edge edge : edges) {
			System.out.println(edge.a + " - " + edge.b);
		}
	}
}
