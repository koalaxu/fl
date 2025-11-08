package fl.data;

import java.io.Serializable;

public class MatchStats implements Serializable {
	private static final long serialVersionUID = 1L;
	public int start_time;
	public int end_time;
	public int pass;
	public int pass_succeed;
	public int key_pass;
	public int dribble;
	public int dribble_succeed;
	public int shot;
	public int shot_ontarget;
	public int header;
	public int header_succeed;
	public int intecept;
	public int tackle;
	public int clearance;
	public int save;
	public int goal_conceded;
	public int goal;
	public int assistance;
	public int foul;
	public int fouled;
	public int yellow;
	public int red;
}
