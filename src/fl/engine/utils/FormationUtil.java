package fl.engine.utils;

import java.util.ArrayList
;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import fl.data.Formation;
import fl.data.Position;
import fl.utils.Point;

public class FormationUtil {
	public static List<Position> GetFormationPositions(Formation formation) {
		if (formation_positions_ == null) Init();
		return formation_positions_.get(formation);
	}
	
	public static List<Point> GetFormationCoordinates(Formation formation) {
		if (formation_coordinates_ == null) Init();
		return formation_coordinates_.get(formation);
	}
	
	private static void Init() {
		formation_positions_ = new HashMap<Formation, List<Position>>();
		formation_coordinates_ = new HashMap<Formation, List<Point>>();
		for (Formation formation : transformations.keySet()) {
			List<Position> positions = new ArrayList<Position>();
			List<Point> coordinates = new ArrayList<Point>();
			AssignFormationPositions(formation, positions, coordinates);
			formation_positions_.put(formation, positions);
			formation_coordinates_.put(formation, coordinates);
		}
	}
	
	private static void  AssignFormationPositions(
			Formation formation, List<Position> positions, List<Point> coordinates) {
		positions.add(Position.GoalKeeper);
		coordinates.add(new Point(0.5, 0.015));
		AssignDefenders(formation.defenders, positions, coordinates);
		AssignMidFielders(formation.midfielders, positions, coordinates);
		AssignAttackers(formation.attackers, positions, coordinates);
	}
	
	private static void AssignDefenders(
			Formation.DefenderFormation def, List<Position> pos, List<Point> co) {
		switch (def) {
		case THREE_DEFENDERS:
			pos.add(Position.CentreBack);
			pos.add(Position.CentreBack);
			pos.add(Position.CentreBack);
			co.add(new Point(0.25, 0.3));
			co.add(new Point(0.5, 0.3));
			co.add(new Point(0.75, 0.3));
			return;
		case FOUR_DEFENDERS:
			pos.add(Position.LeftBack);
			pos.add(Position.CentreBack);
			pos.add(Position.CentreBack);
			pos.add(Position.RightBack);
			co.add(new Point(0.125, 0.35));
			co.add(new Point(0.375, 0.3));
			co.add(new Point(0.625, 0.3));		
			co.add(new Point(0.875, 0.35));
			return;
		case FIVE_DEFENDERS:
			pos.add(Position.LeftBack);
			pos.add(Position.CentreBack);
			pos.add(Position.Sweeper);
			pos.add(Position.CentreBack);
			pos.add(Position.RightBack);
			co.add(new Point(0.1, 0.35));
			co.add(new Point(0.3, 0.3));
			co.add(new Point(0.5, 0.25));
			co.add(new Point(0.7, 0.3));		
			co.add(new Point(0.9, 0.35));
		}
	}
	
	private static void AssignMidFielders(
			Formation.MidFielderFormation mid, List<Position> pos, List<Point> co) {
		switch (mid) {
		case THREE_MIDFIELDERS:
			pos.add(Position.CentralMidFielder);
			pos.add(Position.CentralMidFielder);
			pos.add(Position.CentralMidFielder);
			co.add(new Point(0.25, 0.6));
			co.add(new Point(0.5, 0.6));
			co.add(new Point(0.75, 0.6));
			return;
		case FOUR_MIDFIELDERS_PARALLEL:
			pos.add(Position.LeftMidFielder);
			pos.add(Position.CentralMidFielder);
			pos.add(Position.CentralMidFielder);
			pos.add(Position.RightMidFielder);
			co.add(new Point(0.125, 0.65));
			co.add(new Point(0.375, 0.55));
			co.add(new Point(0.625, 0.55));		
			co.add(new Point(0.875, 0.65));
			return;
		case FOUR_MIDFIELDERS_DIAMOND:
			pos.add(Position.LeftMidFielder);
			pos.add(Position.DefensiveMidFielder);
			pos.add(Position.AttackingMidFielder);
			pos.add(Position.RightMidFielder);
			co.add(new Point(0.25, 0.6));
			co.add(new Point(0.5, 0.5));
			co.add(new Point(0.5, 0.7));
			co.add(new Point(0.75, 0.6));
			return;
		case FIVE_MIDFIELDERS_2_3:
			pos.add(Position.LeftMidFielder);
			pos.add(Position.DefensiveMidFielder);
			pos.add(Position.AttackingMidFielder);
			pos.add(Position.DefensiveMidFielder);
			pos.add(Position.RightMidFielder);
			co.add(new Point(0.1, 0.6));
			co.add(new Point(0.3, 0.5));
			co.add(new Point(0.5, 0.7));
			co.add(new Point(0.7, 0.5));		
			co.add(new Point(0.9, 0.6));
			return;
		case FIVE_MIDFIELDERS_3_2:
			pos.add(Position.LeftMidFielder);
			pos.add(Position.AttackingMidFielder);
			pos.add(Position.DefensiveMidFielder);
			pos.add(Position.AttackingMidFielder);
			pos.add(Position.RightMidFielder);
			co.add(new Point(0.1, 0.6));
			co.add(new Point(0.3, 0.7));
			co.add(new Point(0.5, 0.5));
			co.add(new Point(0.7, 0.7));		
			co.add(new Point(0.9, 0.6));
		}
	}
	
	private static void AssignAttackers(
			Formation.AttackerFormation att, List<Position> pos, List<Point> co) {
		if (att == null) return;
		switch (att) {
		case ONE_ATTACKER:
			pos.add(Position.CentreForward);
			co.add(new Point(0.5, 0.9));
			return;
		case TWO_ATTACKERS:
			pos.add(Position.CentreForward);
			pos.add(Position.SecondStriker);
			co.add(new Point(0.35, 0.9));
			co.add(new Point(0.65, 0.9));
			return;
		case THREE_ATTACKERS:
			pos.add(Position.LeftWinger);
			pos.add(Position.CentreForward);
			pos.add(Position.RightWinger);
			co.add(new Point(0.15, 0.85));
			co.add(new Point(0.5, 0.9));
			co.add(new Point(0.85, 0.85));
		}
	}
	
	private static Map<Formation, List<Position>> formation_positions_;
	private static Map<Formation, List<Point>> formation_coordinates_;
	
	public static class Transform {
		public Transform(int p, Formation f) {
			dismissed_position = p;
			new_formation = f;
		}
		public int dismissed_position;
		public Formation new_formation;
	}
	
	public final static Map<Formation, Transform> transformations = new HashMap<Formation, Transform>() {
		private static final long serialVersionUID = 1L; {
			put(Formation.ThreeFourThree, new Transform(8, Formation.ThreeFourTwo));
			put(Formation.ThreeFiveTwo, new Transform(5, Formation.ThreeFourTwo));
			put(Formation.FourThreeThree, new Transform(8, Formation.FourThreeTwo));
			put(Formation.FourFourTwo, new Transform(9, Formation.FourFourOne));
			put(Formation.FourFourTwoDiamond, new Transform(9, Formation.FourFourOneDiamond));
			put(Formation.FourFiveOne, new Transform(5, Formation.FourFourOneDiamond));
			put(Formation.FiveThreeTwo, new Transform(3, Formation.FourThreeTwo));
			put(Formation.FiveFourOne, new Transform(3, Formation.FourFourOne));
			put(Formation.ThreeFourTwo, new Transform(9, Formation.ThreeFourOne));
			put(Formation.FourThreeTwo, new Transform(9, Formation.FourThreeOne));
			put(Formation.FourFourOne, new Transform(6, Formation.FourThreeOne));
			put(Formation.FourFourOneDiamond, new Transform(7, Formation.FourThreeOne));
			put(Formation.ThreeFourOne, new Transform(5, Formation.ThreeThreeOne));
			put(Formation.FourThreeOne, new Transform(2, Formation.ThreeThreeOne));
			put(Formation.ThreeThreeOne, new Transform(7, Formation.ThreeThreeZero));
			put(Formation.ThreeThreeZero, null);
		}
	};
	
	public static class FormationSetting {
		public FormationSetting(int kickoff_kicker, int[] corner_defenders) {
			this.kickoff_kicker = kickoff_kicker;
			this.corner_defenders = new TreeSet<Integer>();
			for (int i : corner_defenders) this.corner_defenders.add(i);
		}
		public int kickoff_kicker;
		public SortedSet<Integer> corner_defenders;
	}
	
	public final static Map<Formation, FormationSetting> settings = new HashMap<Formation, FormationSetting>() {
		private static final long serialVersionUID = 1L; {
			put(Formation.ThreeFourThree, new FormationSetting(9, new int[]{4, 7}));
			put(Formation.ThreeFiveTwo, new FormationSetting(8, new int[]{4 ,8}));
			put(Formation.FourThreeThree, new FormationSetting(9, new int[]{1 ,4}));
			put(Formation.FourFourTwo, new FormationSetting(8, new int[]{1 ,4}));
			put(Formation.FourFourTwoDiamond, new FormationSetting(8, new int[]{1 ,4}));
			put(Formation.FourFiveOne, new FormationSetting(10, new int[]{1 ,4}));
			put(Formation.FiveThreeTwo, new FormationSetting(9, new int[]{1 ,5}));
			put(Formation.FiveFourOne, new FormationSetting(10, new int[]{1 ,5}));
			put(Formation.ThreeFourTwo, new FormationSetting(8, new int[]{4 ,7}));
			put(Formation.FourThreeTwo, new FormationSetting(8, new int[]{1 ,4}));
			put(Formation.FourFourOne, new FormationSetting(9, new int[]{1 ,4}));
			put(Formation.FourFourOneDiamond, new FormationSetting(9, new int[]{1 ,4}));
			put(Formation.ThreeFourOne, new FormationSetting(8, new int[]{4 ,7}));
			put(Formation.FourThreeOne, new FormationSetting(8, new int[]{1 ,4}));
			put(Formation.ThreeThreeOne, new FormationSetting(7, new int[]{1 ,2, 3}));
			put(Formation.ThreeThreeZero, new FormationSetting(5, new int[]{1 ,2, 3}));
		}
	};	
}
