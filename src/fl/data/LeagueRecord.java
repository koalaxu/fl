package fl.data;

import java.io.Serializable;
import java.util.ArrayList;

public class LeagueRecord implements Serializable  {
	private static final long serialVersionUID = 1L;
	public long league_id;
	public long year;
	public LeagueTable league_table = new LeagueTable();
	public LeagueTable group_table;
	public long[] rank;
	public ArrayList<Long> team_ids;
	
	public long domestic_income;
	public long international_income;
}
