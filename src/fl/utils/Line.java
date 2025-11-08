package fl.utils;

public class Line {
	// ax + by + c = 0;
	public Line(double a, double b, double c) {
		this.a = a;
		this.b = b;
		this.c = c;
	}
	
//	public Vector GetDirectoinalVector() {
//		return new Vector(b, -a);
//	}
//	
//	public Point AnyPoint() {
//		if (a == 0.0) {
//			return new Point(0.0, - c / b);
//		} else if (b == 0) {
//			return new Point(- c / a, 0.0);
//		}
//		return new Point(- c / (2 * a), - c / (2 * b));
//	}
	
	protected double a;
	protected double b;
	protected double c;
}
