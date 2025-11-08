package fl.tools;

import fl.data.Event;
import fl.data.Event.EventType;
import fl.data.League;
import fl.data.Schedule;
import fl.data.utils.FileUtil;
import fl.data.utils.JsonUtil;

import java.util.ArrayList;
import java.util.List;

public class ScheduleGenerator {
	static public Schedule ArrangeDomesticLeague(int num_team) {
		return ArrangeRoundRobin(num_team, false);
	}
	
	static public Schedule ArrangeCampionsLeague() {
		Schedule group_schedule = ArrangeRoundRobin(4, false);
		Schedule schedule = new Schedule();
		schedule.rounds = new Schedule.Round[13];
		for (int j = 0; j < group_schedule.rounds.length; ++j) {
			int num_match = group_schedule.rounds[j].matches.length;
			schedule.rounds[j] = schedule.new Round();
			schedule.rounds[j].matches = new Schedule.Match[num_match * 8];
			for (int k = 0; k < num_match; ++k) {
				for (int i = 0; i < 8; ++i) {
					int group_offset = i * 4;
					Schedule.Match match = schedule.new Match();
					schedule.rounds[j].matches[k + i * num_match] = match;
					match.host = group_schedule.rounds[j].matches[k].host + group_offset;
					match.away = group_schedule.rounds[j].matches[k].away + group_offset;
				}
			}
		}
		ArrangeKnockOut(8, group_schedule.rounds.length, 32, schedule);
		return schedule;
	}
	
	static public Schedule ArrangeDomesticCup() {
		Schedule schedule = new Schedule();
		schedule.rounds = new Schedule.Round[11];
		ArrangeOneRoundKnockOut(4, 0, 0, schedule);   // Winners are 8, 9, 10, 11
		ArrangeKnockOut(16, 2, 8, schedule);
		return schedule;
	}
	
	static public Schedule ArrangeSuperCup() {
		Schedule schedule = new Schedule();
		schedule.rounds = new Schedule.Round[1];
		ArrangeKnockOut(1, 0, 0, schedule);
		return schedule;
	}
	
	static private void ArrangeKnockOut(int knockout, int round_offset, int team_offset, Schedule schedule) {
		while (knockout > 1) {
			ArrangeOneRoundKnockOut(knockout, round_offset, team_offset, schedule);
			round_offset += 2;
			team_offset += knockout * 2;
			knockout = knockout / 2;
		}
		schedule.rounds[round_offset] = schedule.new Round();
		schedule.rounds[round_offset].matches = new Schedule.Match[1];
		schedule.rounds[round_offset].matches[0] = schedule.new Match();
		schedule.rounds[round_offset].matches[0].host = team_offset;
		schedule.rounds[round_offset].matches[0].away = team_offset + 1;
		schedule.rounds[round_offset].matches[0].neutral_site = true;
		schedule.rounds[round_offset].matches[0].winner_id = team_offset + 2;
	}
	
	static private void ArrangeOneRoundKnockOut(int knockout, int round_offset, int team_offset, Schedule schedule) {
		schedule.rounds[round_offset] = schedule.new Round();
		schedule.rounds[round_offset + 1] = schedule.new Round();
		schedule.rounds[round_offset].matches = new Schedule.Match[knockout];
		schedule.rounds[round_offset + 1].matches = new Schedule.Match[knockout];
		for (int i = 0; i < knockout; ++i) {
			schedule.rounds[round_offset].matches[i] = schedule.new Match();
			schedule.rounds[round_offset].matches[i].host = i + team_offset;
			schedule.rounds[round_offset].matches[i].away = i + team_offset + knockout;
			schedule.rounds[round_offset + 1].matches[i] = schedule.new Match();
			schedule.rounds[round_offset + 1].matches[i].away = i + team_offset;
			schedule.rounds[round_offset + 1].matches[i].host = i  + team_offset + knockout;
			schedule.rounds[round_offset + 1].matches[i].winner_id = team_offset + knockout * 2 + i;
		}
	}
	
	static private Schedule ArrangeRoundRobin(int num_team, boolean mirror) {
		int num_round = num_team - 1;
		int num_match = num_team / 2;
		Schedule schedule = new Schedule();
		schedule.rounds = new Schedule.Round[num_round * 2];
		int[] host_away = new int[num_team];
		for (int i = 0; i < num_round; ++i) {
			schedule.rounds[i] = schedule.new Round();
			schedule.rounds[i].matches = new Schedule.Match[num_match];
			for (int j = 0; j < num_match; ++j) {
				Schedule.Match match = schedule.new Match();
				schedule.rounds[i].matches[j] = match;
				int a = GetShiftedPosition(i, j, num_round);
				int b = GetShiftedPosition(i, num_team - 1 - j, num_round);
				if ((j == 0 && (i % 2) == 1) || (j > 0 && (j % 2) == 0)) {
					match.host = a;
					match.away = b;
				} else {
					match.host = b;
					match.away = a;					
				}
				host_away[match.host] |= (1 << i);
			}
		}
		for (int i = 0; i < num_round; ++i) {
			int ref_round = mirror ? num_round - 1 - i : i;
			schedule.rounds[num_round + i] = schedule.new Round();
			schedule.rounds[num_round + i].matches = new Schedule.Match[num_match];
			for (int j = 0; j < num_match; ++j) {
				Schedule.Match match = schedule.new Match();
				schedule.rounds[num_round + i].matches[j] = match;
				match.host = schedule.rounds[ref_round].matches[j].away;
				match.away = schedule.rounds[ref_round].matches[j].host;
			}
		}
		return schedule;
	}
	
	static private int GetShiftedPosition(int round, int original_index, int num_round) {
		if (original_index == 0) return 0;
		return (((original_index - 1) + num_round - round) % num_round) + 1;
	}
	
	public static List<Event> ArrangeSeason() {
		List<Event> weeks = new ArrayList<Event>();
		for (int i = 0; i < 34; ++i) {
			Event calendar = new Event();
			calendar.week_id = i < 17 ? i + 10 : i + 13;
			calendar.type = EventType.MATCH;
			calendar.game_type = League.Type.DOMESTIC_LEAGUE;
			calendar.round = i;
			weeks.add(calendar);
		}
		for (int i = 0; i < 11; ++i) {
			Event calendar = new Event();
			if (i < 2) {
				calendar.week_id = i + 14;  // 14 - 15
			} else if (i < 4) {
				calendar.week_id = i + 15;  // 17 - 18
			} else if (i < 6) {
				calendar.week_id = i + 27; // 31 - 32
			} else if (i < 10) {
				calendar.week_id = i + 29; // 35 - 38
			} else {
				calendar.week_id = 47;
			}
			calendar.type = EventType.MATCH;
			calendar.game_type = League.Type.DOMESTIC_CUP;
			calendar.round = i;
			calendar.mid_week = true;
			weeks.add(calendar);
		}		
		for (int i = 0; i < 13; ++i) {
			Event calendar = new Event();
			calendar.mid_week = true;
			if (i < 3) {
				calendar.week_id = i + 20;  // 20 - 22
			} else if (i < 6) {
				calendar.week_id = i + 21;  // 24 - 26
			} else if (i < 8) {
				calendar.week_id = i + 27;  // 33 - 34
 			} else if (i < 10) {
				calendar.week_id = i + 31;  // 39 - 40
 			} else if (i < 12) {
				calendar.week_id = i + 32;  // 42 - 43
 			} else {
 				calendar.week_id = 48;
 				calendar.mid_week = false;
 			}
			calendar.type = EventType.MATCH;
			calendar.game_type = League.Type.CONTINENTAL_CUP;
			calendar.round = i;
			
			weeks.add(calendar);
		}		
		Event calendar = new Event();
		calendar.week_id = 12;
		calendar.type = EventType.MATCH;
		calendar.game_type = League.Type.DOMESTIC_SUPER_CUP;
		calendar.mid_week = true;
		weeks.add(calendar);
		calendar = new Event();
		calendar.week_id = 13;
		calendar.type = EventType.MATCH;
		calendar.game_type = League.Type.CONTINENTAL_SUPER_CUP;
		calendar.mid_week = true;
		weeks.add(calendar);
		calendar = new Event();
		calendar.week_id = 0;
		calendar.type = EventType.YOUTH_PROMOTION;
		weeks.add(calendar);
		calendar = new Event();
		calendar.week_id = 0;
		calendar.mid_week = true;
		calendar.type = EventType.MATCH_DRAW;	
		weeks.add(calendar);
		for (int i = 1; i <= 8; ++i) {
			for (int j = 0; j < 2; ++j) {
				calendar = new Event();
				calendar.week_id = i;
				calendar.mid_week = (j == 1);
				calendar.type = EventType.TRANSFER;
				calendar.round = 15 - ((i - 1) * 2 + j);
				weeks.add(calendar);
			}	
		}
		calendar = new Event();
		calendar.week_id = 9;
		calendar.type = EventType.SQUAD_NUMBER_ASSIGNMENT;	
		weeks.add(calendar);	
		calendar = new Event();
		calendar.week_id = 29;
		calendar.type = EventType.CONTINENTAL_LEAGUE_KNOCKOUT_DRAW;	
		weeks.add(calendar);
		calendar = new Event();
		calendar.week_id = 50;
		calendar.type = EventType.SEASON_END;	
		weeks.add(calendar);	
		calendar = new Event();
		calendar.week_id = 51;
		calendar.type = EventType.CONTRACT_RENEW;	
		weeks.add(calendar);		
		weeks.sort(ScheduleGenerator::CompareWeek);
		return weeks;
	}
	
	private static int CompareWeek(Event a, Event b) {
		if (a.week_id < b.week_id) return -1;
		if (a.week_id > b.week_id) return 1;
		if (!a.mid_week && b.mid_week) return -1;
		if (a.mid_week && !b.mid_week) return 1;
		return 0;
	}
	
	public static void main(String[] args) {
		FileUtil.Init();
		Schedule schedule = ArrangeDomesticLeague(18);
		JsonUtil.WriteOneObjectToJson("constants/league_schedule.json", schedule);
		schedule = ArrangeDomesticCup();
		JsonUtil.WriteOneObjectToJson("constants/cup_schedule.json", schedule);
		schedule = ArrangeSuperCup();
		JsonUtil.WriteOneObjectToJson("constants/super_cup_schedule.json", schedule);
		schedule = ArrangeCampionsLeague();
		JsonUtil.WriteOneObjectToJson("constants/champion_league_schedule.json", schedule);
		
		List<Event> calendar = ArrangeSeason();
		JsonUtil.WriteArrayToJson("constants/calendar.json", calendar, Event.class, false);
	}
}
