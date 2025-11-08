package fl.utils;

public class GeometryUtil {
	public static double Distance(Point p0, Point p1) {
		return Math.sqrt((p0.x - p1.x) * (p0.x - p1.x) + (p0.y - p1.y) * (p0.y - p1.y));
	}
	
	public static Line CreateLine(Point p0, Point p1) {
		return new Line(p1.y - p0.y, p0.x - p1.x, p1.x * p0.y - p1.y * p0.x);
	}
	
	public static LineSegment CreateLineSegment(Point p0, Point p1) {
		return new LineSegment(p0, new Vector(p1.x - p0.x, p1.y - p0.y).Normalize(), Distance(p0, p1));
	}
	
	public static double Distance(Line l, Point p) {
		return Math.abs(l.a * p.x + l.b * p.y + l.c) / Math.sqrt(l.a * l.a  + l.b * l.b);
	}
	
	public static double ProjectedLength(Vector v0, Vector v1) {
		return v0.DotProduct(v1) / v1.Length();
	}
	
	public static Vector GetProjectedVector(Vector v0, Vector v1) {
		Vector v = new Vector(v0.x, v0.y);
		v.Scale(ProjectedLength(v0, v1));
		return v;
	}
	
	public static boolean PointInRectangle(Rectangle rect, Point p) {
		return p.x > rect.top_left.x && p.x < rect.right_bottom.x
				&& p.y > rect.top_left.y && p.y < rect.right_bottom.y;
	}
	
	public static Point Intersect(LineSegment l0, LineSegment l1) {
		if (l0.length == 0.0 || l1.length == 0.0) return null;
		
//		System.err.println("l0=" + l0);
//		System.err.println("l1=" + l1);
		Vector v = l0.directional_vector;
		Vector w = l1.directional_vector;
		Vector u = new Vector(l1.start_point, l0.start_point);
//		System.err.println("u=" + u);
		double d = v.CrossProduct(w);
		if (d == 0.0) return null;
		double len0 = w.CrossProduct(u) / d;
		double len1 = v.CrossProduct(u) / d;
//		System.err.println("len0=" + len0);
//		System.err.println("len1=" + len1);
		if (len0 >= 0 && len0 <= l0.length && len1 >= 0 && len1 <= l1.length) {
			Vector rect = new Vector(v);
			rect.Scale(len0);
			return rect;
		}
		return null;
	}
	
	public static double Angle(Point base, Point a, Point b) {
		Vector v = new Vector(base, a);
		Vector w = new Vector(base, b);
		return Angle(v, w);
	}
	
	public static double Angle(Vector v, Vector w) {
		double len_v = v.Length();
		double len_w = w.Length();
		if (len_v == 0 || len_w == 0) return 0;
		return Math.acos(v.DotProduct(w) / (len_v * len_w));
	}
	
	public static Vector Rotate(Vector v, double angle) {
		double cos = Math.cos(angle);
		double sin = Math.sin(angle);
		Vector u = new Vector(v);
		u.Normalize();
		double x = u.x;
		double y = u.y;
		u.x = cos * x - sin * y;
		u.y = cos * y + sin * x;
		u.Scale(v.Length());
		return u;
	}
	
}
