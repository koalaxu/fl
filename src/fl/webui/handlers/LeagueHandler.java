package fl.webui.handlers;

import java.lang.reflect.Field;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;

import fl.data.Constant;
import fl.data.Event;
import fl.data.League;
import fl.data.MatchStats;
import fl.data.PlayerLeagueRecord;
import fl.data.Schedule;
import fl.data.LeagueInfo;
import fl.engine.data.ClubData;
import fl.engine.data.LeagueData;
import fl.engine.data.LeagueData.TeamSlot;
import fl.engine.data.LeagueRecordData;
import fl.engine.data.LeagueRecordData.LeagueTableData;
import fl.engine.data.MatchData;
import fl.engine.data.PlayerData;
import fl.engine.data.PlayerLeagueData;
import fl.engine.utils.Calendar;
import fl.engine.utils.LeagueUtil;
import fl.engine.utils.ScheduleUtil;
import fl.utils.FieldAccessor;
import fl.utils.StringUtil;
import fl.utils.TopN;
import fl.webui.utils.CompoundElement;
import fl.webui.utils.LinkedItem;
import fl.webui.utils.SplitElement;
import fl.webui.utils.TableElement;
import fl.webui.utils.TableElement.Cell.Alignment;
import fl.webui.utils.TextElement;

public class LeagueHandler extends BaseHandler {
	@Override
	protected boolean GetMenu(List<String> menu_list) {
		menu_list.add("LeagueTable");
		menu_list.add("Fixture");
		menu_list.add("History");
		menu_list.add("PlayerStats");
		menu_list.add("Discipline");
		return true;
	}

	@Override
	protected void HandleRequest(Map<String, String> parameters, Response response) {
		long id = Long.valueOf(parameters.get("id"));
		long year = Long.valueOf(parameters.getOrDefault("year",
				Long.valueOf(accessor.GetGlobal().year).toString()));
		String tab = parameters.getOrDefault("tab", "default");
		LeagueData league = accessor.GetLeague(id);
		
		CompoundElement body = new CompoundElement();
		response.title = league.GetData().name + " - " + year;

		if (tab.equals("default")) {
			BuildLeagueTable(league, year, body);
		} else if (tab.equals("fixture")) {
			BuildFixture(league, year, body);
		} else if (tab.equals("history")) {
			BuildHistory(league, body);
		} else if (tab.equals("playerstats")) {
			BuildPlayerStats(league, year, body);
		} else if (tab.equals("discipline")) {
			BuildDiscipline(league, body);
		}

		response.body = body.ToHtml();
		response.country = league.GetCountry();
	}
	
	private void BuildLeagueTable(LeagueData league, long year, CompoundElement body) {
		LeagueRecordData data = league.GetLeagueRecord(year);
		if (!data.Started()) {
			body.AddElement(new TextElement("Not started yet."));
			return;
		}
		switch (league.GetData().type) {
		case DOMESTIC_LEAGUE:
			PopulateForLeague(data, body);
			break;
		case CONTINENTAL_CUP:
			PopulateForContinentalCup(data, body);
		case DOMESTIC_CUP:
		case DOMESTIC_SUPER_CUP:
		case CONTINENTAL_SUPER_CUP:
			PopulateKnockout(league, data, body);
		}
	}
	
	private void BuildFixture(LeagueData league, long year, CompoundElement body) {
		League.Type league_type = league.GetData().type;
		LeagueInfo info = league.GetInfo();
		Calendar calendar = new Calendar();
		Schedule schedule = Constant.GetConstant().league_schedule.get(league_type);
		for (int i = 0; i < 104; ++i) {
			int week = i / 2;
			boolean mid_week = (i % 2) == 0;
			Event event = calendar.GetEvent(week, mid_week);
			if (event == null || event.game_type != league_type) continue;
			TableElement table = new TableElement(3, true);
			table.SetDefaultAlignment(Alignment.CENTER);
			if (event.round != null) {
				table.AddElement(new TextElement(
					"Round - " + (event.round + 1) + " " + StringUtil.GetTime(week, mid_week))).SetColumnSpan(3);
			} else {
				table.AddElement(new TextElement(StringUtil.GetTime(week, mid_week))).SetColumnSpan(3);
			}
			if (event.round == null) event.round = 0;
			for (int j = 0; j < schedule.rounds[event.round].matches.length; ++j) {
				Schedule.Match scheduled_match = schedule.rounds[event.round].matches[j];
				MatchData match = league.GetMatch((int)year, event.round, j);
				if (match != null && match.GetMatch() != null) {
					table.AddElement(LinkedItem.CreateClubItem(match.GetHomeClub()));
					table.AddElement(LinkedItem.CreateMatchItem(match));
					table.AddElement(LinkedItem.CreateClubItem(match.GetAwayClub()));
				} else if (info.team_ids != null && scheduled_match.host < info.team_ids.size() &&
					scheduled_match.away < info.team_ids.size()) {
					long home_id = info.team_ids.get(scheduled_match.host);
					long away_id = info.team_ids.get(scheduled_match.away);
					if (home_id > 0) table.AddElement(LinkedItem.CreateClubItem(accessor.GetClub(home_id)));
					else table.AddElement(new TextElement("<TBD>"));
					table.AddElement(new TextElement(" ----- "));
					if (away_id > 0) table.AddElement(LinkedItem.CreateClubItem(accessor.GetClub(away_id)));
					else table.AddElement(new TextElement("<TBD>"));
				}
			}
			body.AddElement(table);
		}
	}
	
	private void BuildHistory(LeagueData league, CompoundElement body) {
		SortedMap<Integer, LeagueRecordData> records = league.GetLeagueRecords();
		TableElement table = new TableElement(5, true);
		for (String head : kHistoryHeader) table.AddElement(new TextElement(head));
		for (Entry<Integer, LeagueRecordData> record : records.entrySet()) {
			List<ClubData> ranked_clubs = record.getValue().GetRankedTeams();
			if (!record.getValue().Started() || ranked_clubs == null || ranked_clubs.isEmpty()) continue;
			TextElement row = LinkedItem.CreateLeagueItem(league, record.getKey());
			row.content = record.getKey().toString();
			table.AddElement(row);
			table.AddElement(LinkedItem.CreateClubItem(ranked_clubs.get(0)));
			table.AddElement(LinkedItem.CreateClubItem(ranked_clubs.get(1)));
			if (ranked_clubs.size() > 3) {
				table.AddElement(LinkedItem.CreateClubItem(ranked_clubs.get(2)));
				table.AddElement(LinkedItem.CreateClubItem(ranked_clubs.get(3)));
			} else {
				table.AddElement(new TextElement("/"));
				table.AddElement(new TextElement("/"));
			}
		}
		body.AddElement(table);
	}
	
	private void BuildPlayerStats(LeagueData league, long year, CompoundElement body) {
		Map<Long, PlayerLeagueRecord> player_stats = LeagueUtil.ComputePlayerStats(league.GetMatches(year));
		List<PlayerLeagueRecord> records = new ArrayList<PlayerLeagueRecord>();
		records.addAll(player_stats.values());
		if (records.isEmpty()) return;
		for (int i = 0; i < kPlayerStatsFields.length; ++i) {
			TableElement table = new TableElement(4, true);
			table.AddElement(new TextElement("Rank"));
			table.AddElement(new TextElement("Player"));
			table.AddElement(new TextElement("Club"));
			table.AddElement(new TextElement(kPlayerStatsNames[i]));
			List<PlayerLeagueRecord> ranked_records = TopN.FindSortedTopN(records, 20,
					new PlayerRecordComparator(kPlayerStatsFields[i], kPlayerStatsAverages[i]));
			for (int j = 0; j < ranked_records.size(); ++j) {
				try {
					PlayerLeagueRecord record = ranked_records.get(j);
					int number = record.stats.getClass().getField(kPlayerStatsFields[i]).getInt(record.stats);
					if (number == 0) break;
					table.AddElement(new TextElement(j + 1));
					PlayerData player = accessor.GetPlayer(record.player_id);
					table.AddElement(LinkedItem.CreatePlayerItem(player));
					ClubData club = accessor.GetClub(player.GetAnnualRecord(year).club_id);
					table.AddElement(LinkedItem.CreateClubItem(club));
				

					if (kPlayerStatsAverages[i]) {
						double avg_number = (double)number / record.match_played;
						table.AddElement(new TextElement(String.format("%.1f (%d)", avg_number, record.match_played)));
					} else {
						
						table.AddElement(new TextElement(number));
					}
				} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException
						| SecurityException e) {
					e.printStackTrace();
					System.exit(0);
				}
			}
			body.AddElement(table);
		}
	}
	
	private void BuildDiscipline(LeagueData league, CompoundElement body) {
		TableElement table = new TableElement(3, true);
		table.AddElement(new TextElement("Player"));
		table.AddElement(new TextElement("Club"));
		table.AddElement(new TextElement("Suspended"));
		for (PlayerLeagueData data : league.GetPlayerLeagueInfo()) {
			int suspended = FieldAccessor.kZeroBased.Get(data.GetInfo().suspended);
			if (suspended == 0) continue;
			ClubData club = data.GetPlayer().GetClub();
			if (!league.HasClub(club)) continue;
			table.AddElement(LinkedItem.CreatePlayerItem(data.GetPlayer()));
			table.AddElement(LinkedItem.CreateClubItem(club));
			table.AddElement(new TextElement(suspended));
		}
		body.AddElement(table);
	}
	
	private void PopulateForLeague(LeagueRecordData data, CompoundElement body) {
		TableElement table = new TableElement(9, true);
		for (String head : headers) {
			table.AddElement(new TextElement(head));
		}
		for (int i = 0; i < data.GetSize(); ++i) {
			LeagueTableData stats = data.GetClubStats(i);
			PopulateStatsToTable(stats, i + 1, table);
		}
		body.AddElement(table);
	}
	
	private void PopulateForContinentalCup(LeagueRecordData data, CompoundElement body) {
		for (int group = 0; group < League.kGroupNumber; ++group) {
			TextElement title = new TextElement("Group " + (group + 1));
			title.title = true;
			body.AddElement(title);
			TableElement table = new TableElement(9, true);
			for (String head : headers) {
				table.AddElement(new TextElement(head));
			}
			for (int i = 0; i < League.kTeamPerGroup; ++i) {
				LeagueTableData stats = data.GetGroupClubStats(group, i);
				PopulateStatsToTable(stats, i + 1, table);
			}
			body.AddElement(table);
			body.AddElement(SplitElement.kSplitElement);
		}
	}
	
	private void PopulateStatsToTable(LeagueTableData stats, int rank, TableElement table) {
		table.AddElement(new TextElement(rank));
		table.AddElement(LinkedItem.CreateClubItem(stats.club));
		table.AddElement(new TextElement(stats.stats.win));
		table.AddElement(new TextElement(stats.stats.draw));
		table.AddElement(new TextElement(stats.stats.lose));
		table.AddElement(new TextElement(stats.stats.goal));
		table.AddElement(new TextElement(stats.stats.goal_conceded));
		table.AddElement(new TextElement(stats.GoalDifference()));
		table.AddElement(new TextElement(stats.Points()));
	}
	
	private void PopulateKnockout(LeagueData league, LeagueRecordData data, CompoundElement body) {
		if (!data.Started()) {
			body.AddElement(new TextElement("Not available this year."));
			return;
		}
		int rounds = ScheduleUtil.GetKnockoutRounds(league.GetData().type);
		for (int i = 0; i < rounds; ++i) {
			TextElement title = new TextElement(GetKnockoutName(i, rounds));
			title.title = true;
			body.AddElement(title);
			TableElement table = new TableElement(2, false);
			List<TeamSlot> teams = data.GetTeamsInKnockouts(i);
			List<TeamSlot> winners = data.GetWinnersInKnockouts(i);
			for (int j = 0; j < winners.size(); ++j) {
				table.AddElement(FillTeamSlot(teams.get(j * 2 + 0))).SetAlignment(Alignment.CENTER);
				table.AddElement(FillTeamSlot(teams.get(j * 2 + 1))).SetAlignment(Alignment.CENTER);
				table.AddElement(FillTeamSlot(winners.get(j))).SetColumnSpan(2).SetAlignment(Alignment.CENTER);
			}
			body.AddElement(table);
			body.AddElement(SplitElement.kSplitElement);
		}
	}
	
	private static String GetKnockoutName(int round, int total_round) {
		int index = total_round - round - 1;
		if (index < knockouts.length) return knockouts[index];
		return "Round " + round;
	}
	
	private static TextElement FillTeamSlot(TeamSlot team_slot) {
		if (team_slot.club != null) return LinkedItem.CreateClubItem(team_slot.club);
		return FillTeamSlot(team_slot.match_team_id);
	}
	
	private static TextElement FillTeamSlot(long match_team_id) {
		return new TextElement("Team Slot " + match_team_id);
	}
	
	private static class PlayerRecordComparator implements Comparator<PlayerLeagueRecord> {
		public PlayerRecordComparator(String field, boolean average) {
			try {
				field_ = MatchStats.class.getField(field);
				if (average) match_field_ = PlayerLeagueRecord.class.getField("match_played");
			} catch (NoSuchFieldException | SecurityException e) {
				e.printStackTrace();
				System.exit(0);
			}
		}

		@Override
		public int compare(PlayerLeagueRecord o1, PlayerLeagueRecord o2) {
			try {
				double value1 = field_.getInt(o1.stats);
				double value2 = field_.getInt(o2.stats);
				if (match_field_ != null) {
					int match1 = match_field_.getInt(o1);
					int match2 = match_field_.getInt(o2);
					if (match1 >= 10 && match2 < 10) return -1;
					if (match1 < 10 && match2 >= 10) return 1;
					value1 /= match1;
					value2 /= match2;
				}
				if (value1 > value2) return -1;
				if (value1 < value2) return 1;
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
				System.exit(0);
			}
			return 0;
		}
		
		private Field field_;
		private Field match_field_;
	}
	
	private static String[] headers = { "R", "Club", "W", "D", "L", "GF", "GA", "GN", "Pts" };
	private static String[] knockouts = { "Final", "Semi-Final", "Quater-Final", "1/8 Final", "1/16 Final" };
	private static String[] kHistoryHeader = { "Year", "1st", "2nd", "3rd", "4th" };
	private static String[] kPlayerStatsFields = { "goal", "assistance", "save", "key_pass", "dribble_succeed",
			"shot", "intecept", "tackle", "clearance", "yellow", "red" };
	private static boolean[] kPlayerStatsAverages = { false, false, true, true, true, true, true, true, true,
			false, false};
	private static String[] kPlayerStatsNames = { "Goal", "Assistance", "Save", "Key Pass", "Dribble",
			"Shot", "Intecept", "Tackle", "Clearance", "Yellow Card", "Red Card"  };
}
