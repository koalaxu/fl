package fl.data;

import fl.data.Position.PositionName;

public class PlayerInfo {
	public long player_id;
	
	public PositionName position;
	public PositionName secondary_position;
	
	public Ability ability;
	
	public Integer condition;
	public Integer stamina;
	public Integer injury;   // In half-week.
	
	static public int kMaxFootAbility = 3;
	public Integer left_foot;   // 0 - 3
	public Integer right_foot;  // 0 - 3
	
	public Integer exp;
	
	
	public class PlayerClubInfo {
		public int contract;
		public Integer wage;
		public Integer squad_number;
	}
	public long club_id;
	public PlayerClubInfo club_info;
	
	public Position GetPosition() {
		return Position.positions.get(position);
	}
	public Position GetSecondaryPosition() {
		if (secondary_position == null) return null;
		return Position.positions.get(secondary_position);
	}
}