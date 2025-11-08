package fl.data;

import java.util.TreeMap;

public class Formation {
	private Formation(FormationName n, String rn, DefenderFormation d, MidFielderFormation m, AttackerFormation a) {
		defenders = d;
		midfielders = m;
		attackers = a;
		name = n;
		readable_name = rn;
	}
	
	private Formation(DefenderFormation d, MidFielderFormation m, AttackerFormation a) {
		defenders = d;
		midfielders = m;
		attackers = a;
	}
	
	public enum FormationName {
		FORMATION_343,
		FORMATION_352,
		FORMATION_433,
		FORMATION_442,
		FORMATION_442_DIAMOND,
		FORMATION_451,
		FORMATION_532,
		FORMATION_541,
	}
	
	public enum DefenderFormation {
		THREE_DEFENDERS,
		FOUR_DEFENDERS,
		FIVE_DEFENDERS,
	}
	
	public enum MidFielderFormation {
		THREE_MIDFIELDERS,
		FOUR_MIDFIELDERS_PARALLEL,
		FOUR_MIDFIELDERS_DIAMOND,
		FIVE_MIDFIELDERS_3_2,
		FIVE_MIDFIELDERS_2_3,
	}	
	
	public enum AttackerFormation {
		ONE_ATTACKER,
		TWO_ATTACKERS,
		THREE_ATTACKERS,
	}
	
	public FormationName name;
	public String readable_name;
	public DefenderFormation defenders;
	public MidFielderFormation midfielders;
	public AttackerFormation attackers;
	
	public int NumberOfDefenders() {
		switch (defenders) {
		case FIVE_DEFENDERS:
			return 5;
		case FOUR_DEFENDERS:
			return 4;
		case THREE_DEFENDERS:
			return 3;
		}
		return 0;
	}
	
	public int NumberOfMidFielders() {
		switch (midfielders) {
		case FIVE_MIDFIELDERS_2_3:
			return 5;
		case FIVE_MIDFIELDERS_3_2:
			return 5;
		case FOUR_MIDFIELDERS_DIAMOND:
			return 4;
		case FOUR_MIDFIELDERS_PARALLEL:
			return 4;
		case THREE_MIDFIELDERS:
			return 3;
		}
		return 0;
	}
	
	public int NumberOfAttackers() {
		switch (attackers) {
		case ONE_ATTACKER:
			return 1;
		case THREE_ATTACKERS:
			return 3;
		case TWO_ATTACKERS:
			return 2;
		}
		return 0;
	}
	
	public static  Formation ThreeFourThree = new Formation(
			FormationName.FORMATION_343, "3-4-3", DefenderFormation.THREE_DEFENDERS,
			MidFielderFormation.FOUR_MIDFIELDERS_PARALLEL, AttackerFormation.THREE_ATTACKERS);
	public static  Formation ThreeFiveTwo = new Formation(
			FormationName.FORMATION_352, "3-5-2", DefenderFormation.THREE_DEFENDERS,
			MidFielderFormation.FIVE_MIDFIELDERS_2_3, AttackerFormation.TWO_ATTACKERS);	
	public static  Formation FourThreeThree = new Formation(
			FormationName.FORMATION_433, "4-3-3", DefenderFormation.FOUR_DEFENDERS,
			MidFielderFormation.THREE_MIDFIELDERS, AttackerFormation.THREE_ATTACKERS);
	public static  Formation FourFourTwo = new Formation(
			FormationName.FORMATION_442, "4-4-2", DefenderFormation.FOUR_DEFENDERS,
			MidFielderFormation.FOUR_MIDFIELDERS_PARALLEL, AttackerFormation.TWO_ATTACKERS);	
	public static  Formation FourFourTwoDiamond = new Formation(
			FormationName.FORMATION_442_DIAMOND, "4-1-2-1-2", DefenderFormation.FOUR_DEFENDERS,
			MidFielderFormation.FOUR_MIDFIELDERS_DIAMOND, AttackerFormation.TWO_ATTACKERS);	
	public static  Formation FourFiveOne = new Formation(
			FormationName.FORMATION_451, "4-5-1", DefenderFormation.FOUR_DEFENDERS,
			MidFielderFormation.FIVE_MIDFIELDERS_3_2, AttackerFormation.ONE_ATTACKER);
	public static  Formation FiveThreeTwo = new Formation(
			FormationName.FORMATION_532, "5-3-2", DefenderFormation.FIVE_DEFENDERS,
			MidFielderFormation.THREE_MIDFIELDERS, AttackerFormation.TWO_ATTACKERS);	
	public static  Formation FiveFourOne = new Formation(
			FormationName.FORMATION_541, "5-4-1", DefenderFormation.FIVE_DEFENDERS,
			MidFielderFormation.FOUR_MIDFIELDERS_PARALLEL, AttackerFormation.ONE_ATTACKER);	
	
	public final static TreeMap<FormationName, Formation> formations = new TreeMap<FormationName, Formation>() {
		private static final long serialVersionUID = 1L; {
		put(FormationName.FORMATION_343, ThreeFourThree);
		put(FormationName.FORMATION_352, ThreeFiveTwo);
		put(FormationName.FORMATION_433, FourThreeThree);
		put(FormationName.FORMATION_442, FourFourTwo);
		put(FormationName.FORMATION_442_DIAMOND, FourFourTwoDiamond);
		put(FormationName.FORMATION_451, FourFiveOne);
		put(FormationName.FORMATION_532, FiveThreeTwo);
		put(FormationName.FORMATION_541, FiveFourOne);

	}};
	
	// 10 Player
	public static Formation ThreeFourTwo = new Formation(
			DefenderFormation.THREE_DEFENDERS,
			MidFielderFormation.FOUR_MIDFIELDERS_PARALLEL, AttackerFormation.TWO_ATTACKERS);
	public static Formation FourThreeTwo = new Formation(
			DefenderFormation.FOUR_DEFENDERS,
			MidFielderFormation.THREE_MIDFIELDERS, AttackerFormation.TWO_ATTACKERS);
	public static Formation FourFourOne = new Formation(
			DefenderFormation.FOUR_DEFENDERS,
			MidFielderFormation.FOUR_MIDFIELDERS_PARALLEL, AttackerFormation.ONE_ATTACKER);	
	public static Formation FourFourOneDiamond = new Formation(
			DefenderFormation.FOUR_DEFENDERS,
			MidFielderFormation.FOUR_MIDFIELDERS_DIAMOND, AttackerFormation.ONE_ATTACKER);
	
	// 9 Player
	public static Formation ThreeFourOne = new Formation(
			DefenderFormation.THREE_DEFENDERS,
			MidFielderFormation.FOUR_MIDFIELDERS_PARALLEL, AttackerFormation.ONE_ATTACKER);
	public static Formation FourThreeOne = new Formation(
			DefenderFormation.FOUR_DEFENDERS,
			MidFielderFormation.THREE_MIDFIELDERS, AttackerFormation.ONE_ATTACKER);
	
	// 8 Player
	public static Formation ThreeThreeOne = new Formation(
			DefenderFormation.THREE_DEFENDERS,
			MidFielderFormation.THREE_MIDFIELDERS, AttackerFormation.ONE_ATTACKER);
	
	// 7 Player
	public static Formation ThreeThreeZero = new Formation(
			DefenderFormation.THREE_DEFENDERS, MidFielderFormation.THREE_MIDFIELDERS, null);
}
