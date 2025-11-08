package fl.engine.data;

import java.util.ArrayList;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import fl.data.League;
import fl.data.LeagueInfo;
import fl.data.Schedule.Match;
import fl.data.Schedule.Round;
import fl.db.DataBase;
import fl.db.Key;
import fl.engine.data.DataAccessor.Collection;
import fl.engine.utils.ScheduleUtil;

public class LeagueData extends IdKeyedData {
	protected LeagueData(DataBase db, long key) {
		super(db, key);
	}
	
	public static class TeamSlot {
		public TeamSlot(ClubData club) {  this.club = club;  }
		public TeamSlot(int team_id) {  match_team_id = team_id;  }
		public ClubData club;
		public long match_team_id;
	}

	public League GetData() {
		if (league == null) {
			league = db_.league_table.FindRow(new Key(key_));
		}
		return league;
	}
	
	public LeagueInfo GetInfo() {
		if (league_info == null) {
			league_info = db_.league_info_table.FindRow(new Key(key_));
			if (league_info == null) {
				league_info = new LeagueInfo();
				league_info.league_id = key_;
			}
		}
		return league_info;
	}
	
	public void WriteInfo() {
		if (league_info != null) db_.league_info_table.UpdateRow(league_info);
	}
	
	public CountryData GetCountry() {
		if (GetData().country_id == 0) return null;
		return GetAccessor().GetCountry(GetData().country_id);
	}
	
	public List<ClubData> GetTeams() {
		List<ClubData> ret = new ArrayList<ClubData>();
		if (GetInfo() == null || GetInfo().qualified_teams == null) return ret;
		for (long team_id : GetInfo().qualified_teams) {
			ret.add(GetAccessor().GetClub(team_id));
		}
		return ret;
	}
	
	public MatchData GetMatch(int year, int round_id, int match_id) {
		return GetAccessor().GetMatch(key_, round_id, match_id, year);
	}
	
	public boolean HasClub(ClubData club) {
		if (qualified_team_set == null) {
			qualified_team_set = new HashSet<Long>();
			if (GetInfo() != null && GetInfo().qualified_teams != null) {
				qualified_team_set.addAll(GetInfo().qualified_teams);
			}
		}
		return qualified_team_set.contains(club.GetKey());
	}
	
	public List<ClubData> GetTeamsByGroup(int group_id) {
		if (GetData().type != League.Type.CONTINENTAL_CUP) return null;
		if (group_id < 0 || group_id >= League.kGroupNumber || GetInfo().team_ids == null ) return null;
		List<ClubData> ret = new ArrayList<ClubData>();
		for (int i = 0; i < League.kTeamPerGroup; ++i) {
			long team_id = GetInfo().team_ids.get(group_id * League.kTeamPerGroup + i);
			ret.add(GetAccessor().GetClub(team_id));
		}
		return ret;
	}
	
	public int GetGroupByTeam(ClubData club) {
		if (GetData().type != League.Type.CONTINENTAL_CUP) return -1;
		if (GetInfo() == null || GetInfo().team_ids == null) return -1;
		for (int i = 0; i < League.kGroupNumber; ++i) {
			for (int j = 0; j < League.kTeamPerGroup; ++j) {
				if (club.GetKey() == GetInfo().team_ids.get(i * League.kTeamPerGroup + j)) return i;
			}
		}
		return -1;
	}
	
	public List<TeamSlot> GetTeamsInKnockouts(int round_id) {
		if (GetInfo() == null) return null;
		Round round = ScheduleUtil.GetKnockoutRound(GetData().type, round_id);
		if (round == null) return null;
		List<TeamSlot> teams = new ArrayList<TeamSlot>();
		for (Match match : round.matches) {
			AddTeamSlot(match.host, GetInfo().team_ids, teams);
			AddTeamSlot(match.away, GetInfo().team_ids, teams);
		}
		return teams;
	}
	
	public List<TeamSlot> GetWinnersInKnockouts(int round_id) {
		if (GetInfo() == null) return null;
		Round round = ScheduleUtil.GetKnockoutRound(GetData().type, round_id);
		if (round == null) return null;
		List<TeamSlot> teams = new ArrayList<TeamSlot>();
		for (Match match : round.matches) {
			AddTeamSlot(match.winner_id, GetInfo().team_ids, teams);
		}
		return teams;
	}
	
	protected void AddTeamSlot(int match_team_id, List<Long> team_ids, List<TeamSlot> teams) {
		if (match_team_id < team_ids.size()) {
			long team_id = team_ids.get(match_team_id);
			if (team_id > 0) {
				teams.add(new TeamSlot(GetAccessor().GetClub(team_id)));
				return;
			}
		}		
		teams.add(new TeamSlot(match_team_id));
	}
	
	public LeagueRecordData GetLeagueRecord(long year) {
		return new LeagueRecordData(db_, this, year);
	}
	
	public SortedMap<Integer, LeagueRecordData> GetLeagueRecords() {
		SortedSet<Key> row_keys = new TreeSet<Key>();
		db_.league_history_table.GetKeysByForeignKey("league_id", key_, row_keys);
		SortedMap<Integer, LeagueRecordData> ret = new TreeMap<Integer, LeagueRecordData>();
		for (Key key : row_keys) {
			LeagueRecordData record = new LeagueRecordData(db_, this, key);
			ret.put(record.GetYear(), record);
		}
		return ret;
	}
	
	public Collection<MatchData> GetMatches(long year) {
		SortedSet<Key> match_keys = new TreeSet<Key>();
		db_.match_table.GetKeysByForeignKey("league_id", key_, match_keys);
		db_.match_table.FilterKeysByForeignKey("year", year, match_keys);
		return GetAccessor().GetMatches(match_keys.iterator());
	}
	
	public List<PlayerLeagueData> GetPlayerLeagueInfo() {
		List<PlayerLeagueData> ret = new ArrayList<PlayerLeagueData>();
		SortedSet<Key> player_league_keys = new TreeSet<Key>();
		db_.player_league_info_table.GetKeysByForeignKey("league_id", key_, player_league_keys);
		for (Key key : player_league_keys) {
			ret.add(new PlayerLeagueData(db_, key));
		}
		return ret;
	}
	
	private League league;
	private LeagueInfo league_info;
	public Set<Long> qualified_team_set;
}
