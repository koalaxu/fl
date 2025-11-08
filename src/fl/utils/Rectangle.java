package fl.utils;

public class Rectangle {
	public Rectangle(double x0, double y0, double x1, double y1) {
		top_left = new Point(Math.min(x0, x1), Math.min(y0, y1));
		right_bottom = new Point(Math.max(x0, x1), Math.max(y0, y1));
	}
	public Point top_left;
	public Point right_bottom;
}
