package fl.data;

import java.io.Serializable;

public class Match implements Serializable {
	private static final long serialVersionUID = 1L;
	public long year;
	public long league_id;
	public long round_id;
	public long match_id;
	public long home_club;
	public long away_club;
	public boolean knockout;
	public boolean neutral_site;
	
	public MatchResult result = new MatchResult();
}
