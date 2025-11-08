package fl.engine.utils;

import java.util.Comparator;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import fl.data.League.Type;
import fl.data.LeagueTable;
import fl.data.Match;
import fl.data.MatchResult;
import fl.data.MatchStats;
import fl.data.PlayerLeagueRecord;
import fl.engine.data.ClubData;
import fl.engine.data.DataAccessor.Collection;
import fl.engine.data.LeagueData;
import fl.engine.data.LeagueRecordData.LeagueTableData;
import fl.utils.RandomUtil;
import fl.engine.data.MatchData;

public class LeagueUtil {
	public LeagueUtil(LeagueData league) {
		this.league = league;
	}
	
	public static class FirstLegGoals {
		public int home_goals;
		public int away_goals;
	}
	
	public static class TeamResult {
		public TeamResult(int goals, int goals_conceded) {
			this.goals = goals;
			this.goals_conceded = goals_conceded;
			if (goals > goals_conceded) outcome = Outcome.WIN;
			else if (goals < goals_conceded) outcome = Outcome.LOSE;
		}
		public static enum Outcome {
			DRAW,
			WIN,
			LOSE,
		}
		public Outcome outcome = Outcome.DRAW;
		public int goals;
		public int goals_conceded;
	}
	
	public static FirstLegGoals GetFirstLegGoals(MatchResult first_leg_result) {
		if (first_leg_result == null) return null;
		int home_goals = first_leg_result.home.stats.goals;
		int away_goals = first_leg_result.away.stats.goals;
		if (first_leg_result.home.stats.abandoned && away_goals - home_goals < 3) {
			home_goals = 0;
			away_goals = 3;
		} else if (first_leg_result.away.stats.abandoned && home_goals - away_goals < 3) {
			home_goals = 3;
			away_goals = 0;
		}
		FirstLegGoals goals = new FirstLegGoals();
		goals.home_goals = home_goals;
		goals.away_goals = away_goals;
		return goals;
	}
	
	public static void ComputeLeagueTable(List<MatchData> matches, LeagueTable table) {
		Map<Long, LeagueTable.ClubStats> stats = new TreeMap<Long, LeagueTable.ClubStats>();
		for (LeagueTable.ClubStats club_stats : table.clubs) {
			stats.put(club_stats.club_id, club_stats);
		}
		for (MatchData match_data : matches) {
			Match match = match_data.GetMatch();
			MatchResult result = match_data.GetResult();
			TeamResult[] team_results = GetTeamResults(result);
			for (int i = 0; i < 2; ++i) {
				long club_id = i == 0 ? match.home_club : match.away_club;
				LeagueTable.ClubStats club_stats = stats.get(club_id);
				if (club_stats == null) {
					club_stats = new LeagueTable.ClubStats();
					table.clubs.add(club_stats);
					stats.put(club_id, club_stats);
				}
				if (team_results[i].outcome == TeamResult.Outcome.WIN) {
					club_stats.win++;
				} else if (team_results[i].outcome == TeamResult.Outcome.DRAW) {
					club_stats.draw++;
				} else {
					club_stats.lose++;
				}
				club_stats.goal += team_results[i].goals;
				club_stats.goal_conceded += team_results[i].goals_conceded;
				club_stats.club_id = club_id;
			}
		}
	}
	
	public static Map<Long, PlayerLeagueRecord> ComputePlayerStats(Collection<MatchData> matches) {
		Map<Long, PlayerLeagueRecord> player_stats = new HashMap<Long, PlayerLeagueRecord>();
		for (MatchData match_data : matches) {
			MatchResult result = match_data.GetResult(); 
			int max_score = 0;
			long mom = 0;
			for (int i = 0; i < 2; ++i) {
				MatchResult.TeamData team_data = i == 0 ? result.home : result.away;
				double score_adjustment = MatchStatsUtil.GetScoreAdjustment(result, i == 0);
				for (Entry<Long, MatchStats> stats : team_data.player_stats.entrySet()) {
					long player_id = stats.getKey();
					PlayerLeagueRecord record = player_stats.get(player_id);
					if (record == null) {
						record = new PlayerLeagueRecord();
						record.player_id = player_id;
						player_stats.put(player_id, record);
					}
					MatchStats match_stats = stats.getValue();
					MatchStatsUtil.AggregatePlayerStats(match_stats, record.stats);
					record.match_played++;
					record.time_played += (match_stats.end_time - match_stats.start_time);
					int raw_score = MatchStatsUtil.GetRawScore(match_stats, score_adjustment);
					int score = MatchStatsUtil.GetScore(raw_score);
					record.average_score += score;
					if (raw_score >= max_score) {
						max_score = raw_score;
						mom = player_id;
					}
				}
				for (long player_id : team_data.lineup) {
					player_stats.get(player_id).lineup_match_playered++;
				}
			}
			player_stats.get(mom).mom++;
		}
		return player_stats;
	}
	
	public void ComputeFanBase() {
		fan_base = 0L;
		total_fan_base = 0L;
		for (ClubData club : league.GetTeams()) {
			fan_base += club.GetInfo().domestic_fans;
			total_fan_base += club.GetInfo().domestic_fans + club.GetInfo().international_fans;
		}
	}
	
	public void UpdateFanChanges(ClubData home, ClubData away, long winner_id, MatchData match) {
		MatchResult result = match.GetResult();
		boolean home_advantage = home.GetInfo().domestic_fans > away.GetInfo().domestic_fans * kAdvantageMultiplier;
		boolean away_advantage = away.GetInfo().domestic_fans > home.GetInfo().domestic_fans * kAdvantageMultiplier;
		int home_goal = result.home.stats.goals;
		int away_goal = result.away.stats.goals;
		TeamResult[] team_results = GetTeamResults(result);
		ClubData[] clubs = { home, away };
		boolean[] rival_advantage = { away_advantage, home_advantage };
		for (int i = 0; i < 2; ++i) {
			if (league.GetData().type == Type.DOMESTIC_LEAGUE) {
				UpdateFanChangesForDomesticLeague(clubs[i], rival_advantage[i], team_results[i]);
			} else if (league.GetData().type == Type.DOMESTIC_CUP) {
				UpdateFanChangesForDomesticCup(clubs[i], rival_advantage[i], team_results[i],
						match.GetMatch().knockout && winner_id == clubs[i].GetKey(), match.GetMatch().round_id);
			} else if (league.GetData().type == Type.CONTINENTAL_CUP) {
				UpdateFanChangesForContinetnalCup(
						league.GetData().level, clubs[i], rival_advantage[i], team_results[i], 
						match.GetMatch().knockout && winner_id == clubs[i].GetKey(), match.GetMatch().round_id);		
			}
		}
		
		if (home_goal != away_goal) {
			UpdateInternationalFans(home, away, home_goal, away_goal);
		}
	}
	
	private void UpdateFanChangesForDomesticLeague(ClubData club, boolean rival_advantage, TeamResult result) {
		double multiplier = (result.outcome == TeamResult.Outcome.WIN) ? kLeagueWinnerBase : 0.0;
		multiplier += (result.outcome == TeamResult.Outcome.DRAW && rival_advantage) ? kLeagueDrawBase : 0.0;
		multiplier += kLeagueGoalBase * (result.goals * 2 - result.goals_conceded);
		if (multiplier <= 0.0) return;
		club.GetInfo().domestic_fans += fan_base * multiplier;
		club.WriteInfo();
	}
	
	private void UpdateFanChangesForDomesticCup(ClubData club, boolean rival_advantage, TeamResult result,
			boolean knockout_win, long round) {
		double multiplier = (result.outcome == TeamResult.Outcome.WIN) ? kCupWinnerBase : 0.0;
		multiplier += (result.outcome == TeamResult.Outcome.DRAW && rival_advantage) ? kCupDrawBase : 0.0;
		multiplier += knockout_win ? kCupKnockoutWinnerBase * (round / 2 + 1) : 0;
		if (multiplier <= 0.0) return;
		club.GetInfo().domestic_fans += fan_base * multiplier;
		club.WriteInfo();
	}
	
	private void UpdateFanChangesForContinetnalCup(int level, ClubData club, boolean rival_advantage,
			TeamResult result, boolean knockout_win, long round) {
		double multiplier = (result.outcome == TeamResult.Outcome.WIN) ? kContinentalWinnerBase : 0.0;
		multiplier += (result.outcome == TeamResult.Outcome.DRAW && rival_advantage) ? kContinentalDrawBase : 0.0;
		multiplier += kContinentalGoalBase * (result.goals * 2 - result.goals_conceded);
		multiplier += knockout_win ? this.kContinentalKnockoutWinnerBase * ((round - 5) / 2 + 1) : 0;
		if (multiplier <= 0.0) return;
		multiplier *= kContinentalLevelMultiplier[level];
		club.GetInfo().international_fans += total_fan_base * multiplier;
		club.GetInfo().domestic_fans +=
				club.GetInfo().domestic_fans * multiplier * kContinentalDomesticFanMultiplier;
		club.WriteInfo();
	}
	
	private void UpdateInternationalFans(ClubData home, ClubData away, int home_goal, int away_goal) {
		ClubData winner = (home_goal > away_goal) ? home : away;
		int goals = Math.max(home_goal, away_goal);
		int net_goals = Math.abs(home_goal - away_goal);
		double multiplier = kInternationalWinnerBase + kInternationalGoalBase * (goals + net_goals);
		winner.GetInfo().international_fans +=
				(home.GetInfo().international_fans + away.GetInfo().international_fans) * multiplier;
		winner.WriteInfo();
	}
	
	private static TeamResult[]  GetTeamResults(MatchResult result) {
		TeamResult[] team_results = new TeamResult[2];
		team_results[0] = new TeamResult(result.home.stats.goals, result.away.stats.goals);
		team_results[1] = new TeamResult(result.away.stats.goals, result.home.stats.goals);
		return team_results;
	}
	
	public static class TeamComparator implements Comparator<LeagueTableData> {
		public TeamComparator(LeagueData league, long year) {
			base_seed = ((league.GetKey() * 101) + year) * 101;
		}
		@Override
		public int compare(LeagueTableData o1, LeagueTableData o2) {
			if (o1.group_id < o2.group_id) return -1;
			if (o1.group_id > o2.group_id) return 1;
			if (o1.Points() > o2.Points()) return -1;
			if (o1.Points() < o2.Points()) return 1;
			if (o1.GoalDifference() > o2.GoalDifference()) return -1;
			if (o1.GoalDifference() < o2.GoalDifference()) return 1;
			if (o1.stats.goal > o2.stats.goal) return -1;
			if (o1.stats.goal < o2.stats.goal) return 1;
			return RandomUtil.DeterministicWhetherToHappend(0.5, RandomSeed(o1.club, o2.club)) ? -1 : 1;
		}
		
		private long RandomSeed(ClubData c1, ClubData c2) {
			return (base_seed + c1.GetKey()) * 101 + c2.GetKey();
		}
		private long base_seed;
	}
	
	private LeagueData league;
	private long fan_base;
	private long total_fan_base;
	private double kAdvantageMultiplier = 10.0;
	private double kLeagueWinnerBase = 0.0001;
	private double kLeagueDrawBase = 0.00005;
	private double kLeagueGoalBase = 0.00001;
	private double kCupWinnerBase = 0.00001;
	private double kCupDrawBase = 0.00001;
	private double kCupKnockoutWinnerBase = 0.00002;
	private double kContinentalWinnerBase = 0.00001;
	private double kContinentalDrawBase = 0.00001;
	private double kContinentalGoalBase = 0.000001;
	private double kContinentalKnockoutWinnerBase = 0.00002;
	private double kContinentalDomesticFanMultiplier = 10;
	private double[] kContinentalLevelMultiplier = { 0.0, 1.0, 0.5 };
	private double kInternationalWinnerBase = 0.001;
	private double kInternationalGoalBase = 0.0001;
	
}
