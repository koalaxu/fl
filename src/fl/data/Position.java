package fl.data;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class Position {
	private Position(PositionType type, Location location, PositionName name, String abbreviate) {
		this.type = type;
		this.location = location;
		this.name = name;
		this.abbreviate = abbreviate;
	}
	
	public enum PositionType {
		GOAL_KEEPER,
		SWEEPER,
		CENTRE_BACK,
		WING_BACK,
		DEFENSIVE_MIDFIELDER,
		CENTRAL_MIDFIELDER,
		WIDE_MIDFIELDER,
		ATTACKING_MIDFIELDER,
		WINGER,
		CENTRE_FORWARD,
		SECOND_STRIKER,
	}
	public PositionType type;
	
	public enum Location {
		CENTER,
		LEFT,
		RIGHT,
	}
	public Location location;
	public PositionName name;
	public String abbreviate;
	
	public enum PositionCategory {
		GOAL_KEEPER,
		DEFENDER,
		MIDFIELDER,
		FORWARD,
	}
	
	public PositionCategory GetPositionCategory() {
		return GetPositionCategory(this.type);
	}
	public static PositionCategory GetPositionCategory(PositionType type) {
		switch (type) {
		case GOAL_KEEPER:
			return PositionCategory.GOAL_KEEPER;
		case SWEEPER:
		case CENTRE_BACK:
		case WING_BACK:
			return PositionCategory.DEFENDER;
		case DEFENSIVE_MIDFIELDER:
		case CENTRAL_MIDFIELDER:
		case WIDE_MIDFIELDER:
		case ATTACKING_MIDFIELDER:
			return PositionCategory.MIDFIELDER;
		case WINGER:
		case CENTRE_FORWARD:
		case SECOND_STRIKER:
			return PositionCategory.FORWARD;
		}
		return null;
	}
	
	public static Position[] GetPositionsFromType(PositionType type) {
		return kPositionTypeToPositions.get(type);
	}
	
	public Position GetReversePosition() {
		switch (name) {
		case LEFT_BACK:
			return Position.RightBack;
		case RIGHT_BACK:
			return Position.LeftBack;
		case LEFT_MIDFIELDER:
			return Position.RightMidFielder;
		case RIGHT_MIDFIELDER:
			return Position.LeftMidFielder;
		case LEFT_WINGER:
			return Position.RightWinger;
		case RIGHT_WINGER:
			return Position.LeftWinger;
		default:
		}
		return null;
	}
	
	private static Position GetPosition(PositionType type, Location location) {
		for (Position position : positions.values()) {
			if (position.type == type && position.location == location) return position;
		}
		return null;
	}
	
	public enum PositionName {
		GOAL_KEEPER,
		SWEEPER,
		CENTRE_BACK,
		LEFT_BACK,
		RIGHT_BACK,
		DEFENSIVE_MIDFIELDER,
		CENTRAL_MIDFIEDLER,
		LEFT_MIDFIELDER,
		RIGHT_MIDFIELDER,
		ATTACKING_MIDFIELDER,
		CENTRE_FORWARD,
		SECOND_STRIKER,
		LEFT_WINGER,
		RIGHT_WINGER,
	}
	
	public static Position GoalKeeper = new Position(
			PositionType.GOAL_KEEPER, Location.CENTER, PositionName.GOAL_KEEPER, "GK");
	public static Position Sweeper =
			new Position(PositionType.SWEEPER, Location.CENTER, PositionName.SWEEPER, "SW");
	public static Position CentreBack =
			new Position(PositionType.CENTRE_BACK, Location.CENTER, PositionName.CENTRE_BACK, "CB");
	public static Position LeftBack =
			new Position(PositionType.WING_BACK, Location.LEFT, PositionName.LEFT_BACK, "LB");
	public static Position RightBack =
			new Position(PositionType.WING_BACK, Location.RIGHT, PositionName.RIGHT_BACK, "RB");
	public static Position DefensiveMidFielder = new  Position(PositionType.DEFENSIVE_MIDFIELDER, Location.CENTER,
			PositionName.DEFENSIVE_MIDFIELDER, "DM");
	public static Position CentralMidFielder = new  Position(PositionType.CENTRAL_MIDFIELDER, Location.CENTER,
			PositionName.CENTRAL_MIDFIEDLER, "CM");
	public static Position LeftMidFielder = new  Position(PositionType.WIDE_MIDFIELDER, Location.LEFT,
			PositionName.LEFT_MIDFIELDER, "LM");
	public static Position RightMidFielder = new  Position(PositionType.WIDE_MIDFIELDER, Location.RIGHT,
			PositionName.RIGHT_MIDFIELDER, "RM");
	public static Position AttackingMidFielder = new  Position(PositionType.ATTACKING_MIDFIELDER, Location.CENTER,
			PositionName.ATTACKING_MIDFIELDER, "AM");
	public static Position CentreForward = new  Position(PositionType.CENTRE_FORWARD, Location.CENTER,
			PositionName.CENTRE_FORWARD, "CF");
	public static Position SecondStriker = new  Position(PositionType.SECOND_STRIKER, Location.CENTER,
			PositionName.SECOND_STRIKER, "SS");
	public static Position LeftWinger = new  Position(PositionType.WINGER, Location.LEFT,
			PositionName.LEFT_WINGER, "LW");
	public static Position RightWinger = new  Position(PositionType.WINGER, Location.RIGHT,
			PositionName.RIGHT_WINGER, "RW");
	
	final public static Map<PositionName, Position> positions = new TreeMap<PositionName, Position>() {
		private static final long serialVersionUID = 1L; {
		put(PositionName.GOAL_KEEPER, GoalKeeper);
		put(PositionName.SWEEPER, Sweeper);
		put(PositionName.CENTRE_BACK, CentreBack);
		put(PositionName.LEFT_BACK, LeftBack);
		put(PositionName.RIGHT_BACK, RightBack);
		put(PositionName.DEFENSIVE_MIDFIELDER, DefensiveMidFielder);
		put(PositionName.CENTRAL_MIDFIEDLER, CentralMidFielder);
		put(PositionName.LEFT_MIDFIELDER, LeftMidFielder);
		put(PositionName.RIGHT_MIDFIELDER, RightMidFielder);
		put(PositionName.ATTACKING_MIDFIELDER, AttackingMidFielder);
		put(PositionName.CENTRE_FORWARD, CentreForward);
		put(PositionName.SECOND_STRIKER, SecondStriker);
		put(PositionName.LEFT_WINGER, LeftWinger);
		put(PositionName.RIGHT_WINGER, RightWinger);
	}};
	
	final private static Map<PositionType, Position[]> kPositionTypeToPositions =
			new HashMap<PositionType, Position[]>() {
				private static final long serialVersionUID = 1L; {
					for (PositionType type : PositionType.values()) {
						Position[] new_pos = null;
						switch (type) {
						case WING_BACK:
						case WIDE_MIDFIELDER:
						case WINGER:
							new_pos = new Position[2];
							new_pos[0] = GetPosition(type, Location.LEFT);
							new_pos[1] = GetPosition(type, Location.RIGHT);
							break;
						default:
							new_pos = new Position[1];
							new_pos[0] = GetPosition(type, Location.CENTER);
						}
						put(type, new_pos);
					}
				}
			};
}
