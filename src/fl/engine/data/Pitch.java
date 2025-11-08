package fl.engine.data;

import fl.utils.LineSegment;
import fl.utils.Point;
import fl.utils.Rectangle;
import fl.utils.Vector;

public class Pitch {
	// Unit: Feet
	public static double kHalfWidth = 112;
	public static double kWidth = kHalfWidth * 2;
	public static double kHalfLength = 175;
	public static double kLength = kHalfLength * 2;
	public static double kGoalWidth = 24;
	public static double kGoalHeight = 8;
	public static double kBoxWidth = 132;
	public static double kBoxLength = 54;
	public static double kPenaltyLength = 36;
	
	public static double kFreeKickDistance = 30;
	
	public static Rectangle kPitch = new Rectangle(0, 0, Pitch.kWidth, Pitch.kLength);
	public static LineSegment[] kTouchLines = {
			new LineSegment(new Point(0, 0), new Vector(0, 1), kLength),
			new LineSegment(new Point(kWidth, 0), new Vector(0, 1), kLength)
	};
	public static LineSegment[] kGoalLines = {
			new LineSegment(new Point(0, 0), new Vector(1, 0), kWidth),
			new LineSegment(new Point(0, kLength), new Vector(1, 0), kWidth)
	};
	
	public static Point[][] kPost = {
			{ new Point (kHalfWidth - kGoalWidth / 2, 0), new Point (kHalfWidth + kGoalWidth / 2, 0)}, 
			{ new Point (kHalfWidth - kGoalWidth / 2, kLength), new Point (kHalfWidth + kGoalWidth / 2, kLength)}, 
	};
	
	public static Point[] kGoalCenterPoint = { new Point(kHalfWidth, 0), new Point(kHalfWidth, kLength) };
}
