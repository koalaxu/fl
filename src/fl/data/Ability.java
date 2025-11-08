package fl.data;

public class Ability {
	public Integer pace;
	public Integer strength;
	public Integer jump;
	public Integer agility;
	
	public Integer dribble;
	public Integer pass;
	public Integer shot;
	public Integer tackle;
	public Integer position;
	public Integer goalkeep;
	
	static public int kMaxAbility = 20;
	
	public enum Type {
		PACE,
		STRENGTH,
		JUMP,
		AGILITY,
		DRIBBLE,
		PASS,
		SHOT,
		TACKLE,
		POSITION,
		GOALKEEP
	}
	
	static public Type[] kPhysicalAbilties = { Type.PACE, Type.STRENGTH, Type.JUMP, Type.AGILITY };
	static public Type[] kNoGkAbilties = { Type.PACE, Type.STRENGTH, Type.JUMP, Type.AGILITY,
			Type.DRIBBLE, Type.PASS, Type.SHOT, Type.TACKLE, Type.POSITION };
	static public Type[] kGkAbilties = { Type.PACE, Type.STRENGTH, Type.JUMP, Type.AGILITY,
			Type.PASS, Type.TACKLE, Type.GOALKEEP };	
	
	public Integer GetAbility(Type type) {
		switch (type) {
		case PACE:
			return pace;
		case STRENGTH:
			return strength;
		case JUMP:
			return jump;
		case AGILITY:
			return agility;
		case DRIBBLE:
			return dribble;
		case PASS:
			return pass;
		case SHOT:
			return shot;
		case TACKLE:
			return tackle;	
		case POSITION:
			return position;
		case GOALKEEP:
			return goalkeep;			
		}
		return null;
	}
	
	public void SetAbility(Type type, int value) {
		Integer ability = Integer.valueOf(value);
		switch (type) {
		case PACE:
			pace = ability;
			return;
		case STRENGTH:
			strength = ability;
			return;
		case JUMP:
			jump = ability;
			return;
		case AGILITY:
			agility = ability;
			return;
		case DRIBBLE:
			dribble = ability;
			return;
		case PASS:
			pass = ability;
			return;
		case SHOT:
			shot = ability;
			return;
		case TACKLE:
			tackle = ability;
			return;
		case POSITION:
			position = ability;
			return;
		case GOALKEEP:
			goalkeep = ability;
			return;
		}
	}	
}
