package fl.engine.data;

import java.util.ArrayList
;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import fl.data.League;
import fl.data.LeagueInfo;
import fl.data.LeagueRecord;
import fl.data.LeagueTable;
import fl.data.Schedule.Match;
import fl.data.Schedule.Round;
import fl.db.DataBase;
import fl.db.Key;
import fl.engine.data.LeagueData.TeamSlot;
import fl.engine.utils.LeagueUtil;
import fl.engine.utils.ScheduleUtil;

public class LeagueRecordData extends KeyedData {
	protected LeagueRecordData(DataBase db, LeagueData league, long year) {
		super(db, new Key(league.GetKey(), year));
		this.league = league;
		record = db_.league_history_table.FindRow(key_);
		UpdateClubStats();
	}
	
	protected LeagueRecordData(DataBase db, LeagueData league, Key key) {
		super(db, key);
		this.league = league;
		record = db_.league_history_table.FindRow(key_);
		UpdateClubStats();
	}
	
	public static class LeagueTableData {
		public LeagueTableData(LeagueTable.ClubStats stats, ClubData club) {
			this.stats = stats;
			this.club = club;
		}
		public LeagueTable.ClubStats stats;
		public ClubData club;
		public int Matches() {
			return stats.win + stats.draw + stats.lose;
		}
		public int Points() {
			return stats.win * 3 + stats.draw;
		}
		public int GoalDifference() {
			return stats.goal - stats.goal_conceded;
		}
		public int group_id = 0;
	}
	
	public boolean Started() {
		return record != null;
	}
	
	public int GetYear() {
		return (int) key_.keys[1];
	}
	
	public int GetSize() {
		return club_stats.size();
	}
	
	public LeagueTableData GetClubStats(int i) {
		return club_stats.get(i);
	}
	
	public LeagueTableData GetClubStats(ClubData club) {
		for (LeagueTableData data : club_stats) {
			if (data.club.equals(club)) return data;
		}
		return null;
	}
	
	public LeagueTableData GetGroupClubStats(int group_id, int index) {
		return club_stats.get(group_id * League.kTeamPerGroup + index); 
	}
	
	public int GetClubRank(ClubData club) {
		if (record.rank == null) return 0;
		for (int i = 0; i < record.rank.length; ++i) {
			if (club.GetKey() == record.rank[i]) return i + 1;
		}
		return 0;
	}
	
	public List<ClubData> GetRankedTeams() {
		if (record.rank == null) return null;
		List<ClubData> teams = new ArrayList<ClubData>();
		for (long club_id : record.rank) {
			teams.add(GetAccessor().GetClub(club_id));
		}
		return teams;
	}
	
	public List<TeamSlot> GetTeamsInKnockouts(int round_id) {
		if (record == null) return null;
		Round round = ScheduleUtil.GetKnockoutRound(league.GetData().type, round_id);
		if (round == null) return null;
		List<TeamSlot> teams = new ArrayList<TeamSlot>();
		for (Match match : round.matches) {
			league.AddTeamSlot(match.host, GetTeamIds(), teams);
			league.AddTeamSlot(match.away, GetTeamIds(), teams);
		}
		return teams;
	}
	
	public List<TeamSlot> GetWinnersInKnockouts(int round_id) {
		if (record == null) return null;
		Round round = ScheduleUtil.GetKnockoutRound(league.GetData().type, round_id);
		if (round == null) return null;
		List<TeamSlot> teams = new ArrayList<TeamSlot>();
		for (Match match : round.matches) {
			league.AddTeamSlot(match.winner_id, GetTeamIds(), teams);
		}
		return teams;
	}
	
	public void ComputeRank() {
		if (!ScheduleUtil.RankAvailable(league) || !Started()) {
			System.err.println("Can not calculate ranks now.");
			System.exit(0);
		}
		record.rank = new long[league.GetInfo().qualified_teams.size()];
		int knockout_teams = 0;
		switch (league.GetData().type) {
		case DOMESTIC_LEAGUE:
			for (int i = 0; i < club_stats.size(); ++i) {
				record.rank[i] = club_stats.get(i).club.GetKey();
			}
			return;
		case DOMESTIC_CUP:
			knockout_teams = 32;
			break;
		case CONTINENTAL_CUP:
			knockout_teams = 16;
			break;
		case DOMESTIC_SUPER_CUP:
		case CONTINENTAL_SUPER_CUP:
			knockout_teams = 2;
		}
		RankKnockouts(knockout_teams);
	}
	
	public void SetDomesticIncome(long income) {
		record.domestic_income = income;
	}
	
	public void SetInternationalIncome(long income) {
		record.international_income = income;
	}
	
	public void Write(LeagueInfo league_info) {
		UpdateLeagueTable();
		record.team_ids = league_info.team_ids;
		db_.league_history_table.UpdateRow(record);
	}
	
	public void Write() {
		db_.league_history_table.UpdateRow(record);
	}
	
	private void RankKnockouts(int knockout_teams) {
		LeagueInfo info = league.GetInfo();
		Set<Long> qualified_teams = new HashSet<Long>();
		qualified_teams.addAll(info.qualified_teams);
		int index = info.team_ids.size() - 1;
		Map<Long, LeagueTableData> club_stats_map = new HashMap<Long, LeagueTableData>();
		List<LeagueTableData> candidates = new ArrayList<LeagueTableData>();
		for (LeagueTable.ClubStats stats : record.league_table.clubs) {
			club_stats_map.put(stats.club_id, new LeagueTableData(stats, GetAccessor().GetClub(stats.club_id)));
		}
		int ranked_teams = 0;
		for (int i = 1; !qualified_teams.isEmpty(); i *= 2) {
			candidates.clear();
			if (ranked_teams < knockout_teams) {
				for (int j = 0; j < i; ++j, --index) {
					if (qualified_teams.contains(info.team_ids.get(index))) {
						candidates.add(club_stats_map.get(info.team_ids.get(index)));
					}
				}
			} else {
				for (Long club_id : qualified_teams) {
					candidates.add(club_stats_map.get(club_id));
				}	
			}
			candidates.sort(new LeagueUtil.TeamComparator(league, GetYear()));
			for (LeagueTableData data : candidates) {
				record.rank[ranked_teams++] = data.club.GetKey();
				qualified_teams.remove(data.club.GetKey());
			}
			
		}
	}
	
	private void UpdateClubStats() {
		if (record == null) return;
		club_stats.clear();
		switch (league.GetData().type) {
		case DOMESTIC_LEAGUE:
			UpdateForLeague();
			return;
		case CONTINENTAL_CUP:
			UpdateForContinentalCup();
			return;
		default:
		}
	}
	
	private void UpdateLeagueTable() {
		switch (league.GetData().type) {
		case DOMESTIC_LEAGUE:
			UpdateTableForLeague();
			return;
		case CONTINENTAL_CUP:
			UpdateTableForContinentalCup();
			return;
		default:
		}
	}
	
	public void UpdateFromMatches(List<MatchData> matches, int round_id) {
		if (record == null) {
			record = new LeagueRecord();
			record.league_id = league.GetKey();
			record.year = GetYear();
		}
		LeagueUtil.ComputeLeagueTable(matches, record.league_table);
		final int kGroupRounds = League.kTeamPerGroup * (League.kTeamPerGroup - 1) / 2;
		if (league.GetData().type == League.Type.CONTINENTAL_CUP && round_id < kGroupRounds) {
			if (record.group_table == null) record.group_table = new LeagueTable();
			LeagueUtil.ComputeLeagueTable(matches, record.group_table);
		}
		UpdateClubStats();
	}
	
	private void UpdateForLeague() {
		for (LeagueTable.ClubStats stats : record.league_table.clubs) {
			club_stats.add(new LeagueTableData(stats, GetAccessor().GetClub(stats.club_id)));
		}
		if (GetYear() != GetAccessor().GetGlobal().year) return;
		club_stats.sort(new LeagueUtil.TeamComparator(league, GetYear()));
	}
	
	private void UpdateTableForLeague() {
		record.league_table.clubs.clear();
		for (LeagueTableData data : club_stats) {
			record.league_table.clubs.add(data.stats);
		}
	}
	
	private void UpdateForContinentalCup() {
		if (PastYear()) {
			for (LeagueTable.ClubStats stats : record.group_table.clubs) {
				club_stats.add(new LeagueTableData(stats, GetAccessor().GetClub(stats.club_id)));
			}
			return;
		}
		Map<Long, Integer> team_to_group = new HashMap<Long, Integer>();
		for (int i = 0; i < League.kGroupNumber; ++i) {
			List<ClubData> teams = league.GetTeamsByGroup(i);
			for (ClubData team : teams) {
				team_to_group.put(team.GetKey(), i);
			}
		}
		for (LeagueTable.ClubStats stats : record.group_table.clubs) {
			LeagueTableData data = new LeagueTableData(stats, GetAccessor().GetClub(stats.club_id));
			data.group_id = team_to_group.get(stats.club_id);
			club_stats.add(data);
		}
		club_stats.sort(new LeagueUtil.TeamComparator(league, GetYear()));		
	}
	
	private void UpdateTableForContinentalCup() {
		record.group_table.clubs.clear();
		for (LeagueTableData data : club_stats) {
			record.group_table.clubs.add(data.stats);
		}
	}
	
	private boolean PastYear() {
		return GetYear() != GetAccessor().GetGlobal().year;
	}
	
	private List<Long> GetTeamIds() {
		if (record == null) return null;
		if (PastYear()) return record.team_ids;
		return league.GetInfo().team_ids;
	}
	
	private List<LeagueTableData> club_stats = new ArrayList<LeagueTableData>();
	private LeagueData league;
	private LeagueRecord record;
}
