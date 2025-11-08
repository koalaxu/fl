package fl.engine.data;

import fl.data.Match;
import fl.data.MatchCommentary;
import fl.data.MatchResult;
import fl.data.MatchStats;
import fl.db.DataBase;
import fl.db.Key;
import fl.engine.utils.MatchStatsUtil;

public class MatchData extends KeyedData {

	protected MatchData(DataBase db, Match match) {
		super(db, db.match_table.GetKey(match));
		this.match = match;
	}
	
	protected MatchData(DataBase db, Key key) {
		super(db, key);
	}
	
	public Match GetMatch() {
		if (match == null) {
			match = db_.match_table.FindRow(key_);
		}
		return match;
	}
	
	public MatchResult GetResult() {
		GetMatch();
		return match.result;
	}
	
	public String GetCommentary() {
		MatchCommentary commentary = db_.match_commentary_table.FindRow(key_);
		if (commentary == null) commentary = db_.match_commentary_archive_table.FindRow(key_);
		return commentary == null ? null : commentary.commentary;
	}
	
	public LeagueData GetLeague() {
		return new LeagueData(db_, GetMatch().league_id);
	}
	
	public ClubData GetHomeClub() {
		return new ClubData(db_, GetMatch().home_club);
	}
	
	public ClubData GetAwayClub() {
		return new ClubData(db_, GetMatch().away_club);
	}
	
	public MatchStats GetHomeStats() {
		if (home_stats == null) {
			home_stats = MatchStatsUtil.AggregateTeamStats(GetMatch().result.home);
		}
		return home_stats;
	}
	
	public MatchStats GetAwayStats() {
		if (away_stats == null) {
			away_stats = MatchStatsUtil.AggregateTeamStats(GetMatch().result.away);
		}
		return away_stats;
	}
	
	public void Write() {
		if (match != null) {
			db_.match_table.UpdateRow(match);
		}
	}
	
	public void WriteCommentary(String commentary) {
		if (match != null) {
			MatchCommentary match_commentary = new MatchCommentary();
			db_.match_commentary_table.SetKey(match_commentary, key_);
			match_commentary.commentary = commentary;
			db_.match_commentary_table.UpdateRow(match_commentary);
		}
	}
	
	public void ArchiveCommentary() {
		MatchCommentary commentary = db_.match_commentary_table.FindRow(key_);
		if (commentary != null) {
			db_.match_commentary_archive_table.UpdateRow(commentary);
		}
	}

	private Match match;
	private MatchStats home_stats;
	private MatchStats away_stats;
}
