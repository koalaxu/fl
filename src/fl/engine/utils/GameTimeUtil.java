package fl.engine.utils;

import fl.engine.data.Game;
import fl.engine.data.MatchInfo;
import fl.utils.RandomUtil;

public class GameTimeUtil {
	public static String TimeString(MatchInfo match) {
		switch (match.phase) {
		case FIRST_HALF:
			if (match.time < 45 * 60) {
				return FormatTime(kStandardFormat, match.time);
			}
			return FormatTime(kStandardFormat, 45 * 60) + "+" + FormatTime(kStandardFormat, match.time - 45 * 60);
		case SECOND_HALF:
			if (match.time < 45 * 60) {
				return FormatTime(kStandardFormat, 45 * 60 + match.time);
			}
			return FormatTime(kStandardFormat, 90 * 60) + "+" + FormatTime(kStandardFormat, match.time - 45 * 60);			
		case EXTRA_TIME_FIRST_HALF:
			if (match.time < 15 * 60) {
				return FormatTime(kExtraTimeFormat, 90 * 60 + match.time);
			}
			return FormatTime(kExtraTimeFormat, 105 * 60) + "+" + FormatTime(kStandardFormat, match.time - 15 * 60);
		case EXTRA_TIME_SECOND_HALF:
			if (match.time < 15 * 60) {
				return FormatTime(kExtraTimeFormat, 105 * 60 + match.time);
			}
			return FormatTime(kExtraTimeFormat, 120 * 60) + "+" + FormatTime(kStandardFormat, match.time - 15 * 60);
		case PENALTY_KICK:
			return "120:00+PK";
		}
		return null;
	}
	
	public static void IncTime(MatchInfo match, int inc) {
		match.time += inc;
		if (match.time >= GetEndTime(match.phase) && match.stopage_time < 0) {
			match.stopage_time = RandomUtil.SampleFromUniformDistribution(0, 6) * 60;
		}
	}
	
	public static boolean IsTimeOver(MatchInfo match) {
		//return match.time > 60;
		return match.time > GetEndTime(match.phase) + match.stopage_time;
	}
	
	public static int GetAbsoluteTime(MatchInfo match) {
		switch (match.phase) {
		case FIRST_HALF:
			return match.time;
		case SECOND_HALF:
			return 45 * 60 + match.time;
		case EXTRA_TIME_FIRST_HALF:
			return 90 * 60 + match.time;
		case EXTRA_TIME_SECOND_HALF:
			return 105 * 60 + match.time;
		case PENALTY_KICK:
			break;
		}
		return 120 * 60;
	}
	
	private static int GetEndTime(Game.MatchPhase match_phase) {
		switch (match_phase) {
		case FIRST_HALF:
		case SECOND_HALF:
			return 45 * 60;
		case EXTRA_TIME_FIRST_HALF:
		case EXTRA_TIME_SECOND_HALF:
			return 15 * 60;
		default:
		}
		return 0;
	}
	
	public static void UpdateTimeFromState(Game.Directive directive, MatchInfo match) {
		int time_past = 0;
		switch (directive.state) {
		case KICK_OFF:
			time_past = 30;
			break;
		case THROW_IN:
			time_past = 5;
			break;
		case GOAL_KICK:
			time_past = 15;
			break;
		case CORNER_KICK:
			time_past = 15;
			break;
		case FREE_KICK:
			time_past = 20;
			break;
		case PENALTY_KICK:
			time_past = 25;
			break;
		default:
		}
		if (time_past > 0) IncTime(match, time_past);
	}
	
	public static int SubstitutionTime(int num_players) {
		if (num_players == 0) return 0;
		return 5 + num_players * 5;
	}
	
	private static String FormatTime(String format, long time) {
		return String.format(format, time / 60, time % 60);
	}
	
	private static String kStandardFormat = "%02d:%02d";
	private static String kExtraTimeFormat = "%02d:%02d";
}
