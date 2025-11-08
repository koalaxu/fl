package fl.utils;

public class Vector extends Point {

	public Vector(double x, double y) {
		super(x, y);
	}
	
	public Vector(Point from, Point to) {
		super(to.x - from.x, to.y - from.y);
	}
	
	public Vector(double angle) {
		super(Math.cos(angle), Math.sin(angle));
	}
	
	public Vector(Vector v) {
		super(v.x, v.y);
	}
	
	public double Length() {
		return Math.sqrt(x * x + y * y);
	}
	
	public Vector Normalize() {
		double length = Length();
		x = x / length;
		y = y / length;
		return this;
	}
	
	public double DotProduct(Vector v) {
		return x * v.x + y * v.y;
	}
	
	public double CrossProduct(Vector v) {
		return x * v.y - y * v.x;
	}
	
	public Vector Substract(Vector v) {
		return new Vector(x - v.x, y - v.y);
	}
	
	public Vector Scale(double new_length) {
		double scale = new_length / Length();
		x = x * scale;
		y = y * scale;
		return this;
	}
}
