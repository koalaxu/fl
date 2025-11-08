package fl.data;

import java.util.ArrayList;

public class LeagueInfo {
	public long league_id;
	
	// For DOMESTIC_CUP	and CONTINENTAL_CUP, the order is meaningful to the match arrangement.
	// For DOMESTIC_CUP, the last 8 teams will have one more round and the first 8 teams are seeds.
	// For CONTINENTAL_CUP, its pattern is 444332222222 (champion) or 333333332222(euro).
	public ArrayList<Long> qualified_teams;
	
	// The index is associated with the host/away/winner_id in Schedule.
	public ArrayList<Long> team_ids;
	
	transient public long estimated_income;
}
