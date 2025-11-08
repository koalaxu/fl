package fl.data;

import java.io.Serializable;

public class PlayerLeagueRecord implements Serializable {
	private static final long serialVersionUID = 1L;
	public long player_id;
	public long league_id;
	public long year;
	public MatchStats stats = new MatchStats();
	public int match_played;
	public int lineup_match_playered;
	public long time_played;
	public int average_score;
	public int mom;
}
