package fl.utils;

public class LineSegment {
	public LineSegment(Point start_point, Vector directional_vector, double length) {
		this.start_point = start_point;
		this.directional_vector = directional_vector;
		this.length = length;
	}
	
	protected Point start_point;
	protected Vector directional_vector;
	protected double length;
	@Override
	public String toString() {
		return start_point + " -> " + directional_vector + "(" + length + ")";
	} 
}
