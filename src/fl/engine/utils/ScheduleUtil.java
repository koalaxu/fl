package fl.engine.utils;

import fl.data.Constant;
import fl.data.League;
import fl.data.Schedule;
import fl.data.League.Type;
import fl.data.Schedule.Round;
import fl.engine.data.LeagueData;

public class ScheduleUtil {
	public static int GetKnockoutRounds(League.Type league_type) {
		Schedule schedule = Constant.GetConstant().league_schedule.get(league_type);
		if (league_type == Type.DOMESTIC_LEAGUE) return 0;
		if (league_type == Type.CONTINENTAL_CUP) {
			return (schedule.rounds.length - 6 + 1) / 2;
		}
		return (schedule.rounds.length + 1) / 2;
	}
	
	public static Round GetKnockoutRound(League.Type league_type, int round_id) {
		Schedule schedule = Constant.GetConstant().league_schedule.get(league_type);
		if (league_type == Type.DOMESTIC_LEAGUE) return null;
		round_id = round_id * 2 + ((round_id == GetKnockoutRounds(league_type) - 1) ? 0 : 1);
		if (league_type == Type.CONTINENTAL_CUP) round_id += 6;
		if (round_id >= schedule.rounds.length) return null;
		return schedule.rounds[round_id];
	}
	
	public static boolean RankAvailable(LeagueData league) {
		if (league.GetData().type == Type.DOMESTIC_LEAGUE) return true;
		Schedule schedule = Constant.GetConstant().league_schedule.get(league.GetData().type);
		int final_winner_id = schedule.rounds[schedule.rounds.length - 1].matches[0].winner_id;
		return league.GetInfo().team_ids.size() == final_winner_id + 1;
	}
}
