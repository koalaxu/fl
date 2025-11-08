package fl.utils;


public class Point {
	public Point (double x, double y) {
		Set(x, y);
	}
	
	public Point(Point p) {
		Set(p.x, p.y);
	}
	
	public void Copy(Point that) {
		x = that.x;
		y = that.y;
	}
	public void Set(double x, double y) {
		this.x = x;
		this.y = y;
	}
	public void Set(Point base_point, Vector vector) {
		x = base_point.x + vector.x;
		y = base_point.y + vector.y;
	}
	
	public void Add(Vector vector) {
		x = x + vector.x;
		y = y + vector.y;
	}
	
	@Override
	public String toString() {
		// return String.format("[%f,%f]", x, y);
		return String.format("[%d,%d]", (int)x, (int)y);
	}

	public double x;
	public double y;

}
