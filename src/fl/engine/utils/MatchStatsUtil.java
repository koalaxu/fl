package fl.engine.utils;

import java.lang.reflect.Field;

import fl.data.MatchResult;
import fl.data.MatchResult.TeamData;
import fl.data.MatchStats;
import fl.data.PlayerLeagueRecord;

public class MatchStatsUtil {
	public static MatchStats AggregateTeamStats(TeamData team_data) {
		MatchStats team_stats = new MatchStats();
		for (MatchStats player_stats : team_data.player_stats.values()) {
			for (Field field : MatchStats.class.getFields()) {
				try {
					field.set(team_stats, field.getInt(team_stats) + field.getInt(player_stats));
				} catch (IllegalArgumentException | IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
		return team_stats;
	}
	
	public static void AggregatePlayerStats(MatchStats match_stats, MatchStats player_stats) {
		for (Field field : MatchStats.class.getFields()) {
			try {
				field.set(player_stats, field.getInt(player_stats) + field.getInt(match_stats));
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void RollUpPlayerStats(PlayerLeagueRecord stats, PlayerLeagueRecord aggregation) {
		aggregation.match_played += stats.match_played;
		aggregation.lineup_match_playered += stats.lineup_match_playered;
		aggregation.time_played += stats.time_played;
		aggregation.average_score += stats.average_score;
		aggregation.mom += stats.mom;
		AggregatePlayerStats(stats.stats, aggregation.stats);
	}
	
	public static int GetScore(MatchStats stats, double score_adjustment) {
		return GetScore(GetRawScore(stats, score_adjustment));
	}
	
	public static int GetScore(int raw_score) {
		return Math.max(10, Math.min(100, raw_score));
	}
	
	public static int GetRawScore(MatchStats stats, double score_adjustment) {
		int score = 55;
		if (stats.pass >= 10) {
			score += stats.pass_succeed * 30 / stats.pass - 15;
		}
		double attack_score = 0;
		attack_score += Math.max(-5, Math.min(10, stats.dribble_succeed - stats.dribble / 3));
		attack_score += Math.max(-5, Math.min(5, stats.shot_ontarget - stats.shot / 5));
		attack_score += Math.min(10, stats.key_pass * 4);
		attack_score += Math.min(10, stats.fouled / 2);
		double defend_score = 0;
		defend_score += Math.min(10, stats.header_succeed / 2);
		defend_score += Math.min(10, stats.tackle * 2);
		defend_score += Math.min(10, stats.intecept * 2);
		defend_score += Math.min(5, stats.clearance);
		if (attack_score > 0 && defend_score > 0) {
			score += Math.max(attack_score, defend_score) + Math.min(attack_score, defend_score) * 0.25;
		} else {
			score += attack_score + defend_score;
		}
		
		score += Math.min(40, stats.goal * 10);
		score += Math.min(40, stats.assistance * 8);
		score += Math.min(40, stats.save * 5);
		score -= Math.min(50, stats.goal_conceded * 5);
		if (stats.save > 0 && stats.goal_conceded == 0) score += 10;
		score -= Math.min(10, stats.foul);
		
		score -= stats.yellow == 0 ? 0 : (stats.yellow == 1 ? 3 : 10);
		score -= stats.red == 0 ? 0 : 15;
		score += score_adjustment * (stats.end_time - stats.start_time) / 60;
		return score;
	}
	
	public static double GetScoreAdjustment(MatchResult result, boolean is_home_team) {
		return kNetGoalScoreAdjustment * (result.home.stats.goals - result.away.stats.goals)
				* (is_home_team ? 1 : -1) / (result.extra_time ? 120 : 90);
	}
	
	public static double kNetGoalScoreAdjustment = 3.0;
}
