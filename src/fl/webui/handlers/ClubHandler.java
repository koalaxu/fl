package fl.webui.handlers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import fl.data.ClubInfo;
import fl.data.ClubRecord;
import fl.data.Constant;
import fl.data.Event;
import fl.data.Event.EventType;
import fl.data.Formation;
import fl.data.Global;
import fl.data.PlayerLeagueRecord;
import fl.data.Position;
import fl.data.Schedule;
import fl.data.Schedule.Match;
import fl.data.League.Type;
import fl.data.LeagueInfo;
import fl.data.PlayerInfo;
import fl.data.PlayerInfo.PlayerClubInfo;
import fl.engine.data.ClubData;
import fl.engine.data.LeagueData;
import fl.engine.data.LeagueRecordData;
import fl.engine.data.PlayerData;
import fl.engine.data.TransferData;
import fl.engine.utils.FormationUtil;
import fl.engine.utils.LeagueUtil;
import fl.engine.utils.PositionAnalyzer;
import fl.utils.FieldAccessor;
import fl.utils.StringUtil;
import fl.webui.utils.ButtonElement;
import fl.webui.utils.CompoundElement;
import fl.webui.utils.FormElement;
import fl.webui.utils.LinkedItem;
import fl.webui.utils.SelectElement;
import fl.webui.utils.SplitElement;
import fl.webui.utils.TableElement;
import fl.webui.utils.TextElement;

public class ClubHandler extends BaseHandler {
	@Override
	protected boolean GetMenu(List<String> menu_list) {
		menu_list.add("Squad");
		menu_list.add("Fixture");
		menu_list.add("PlayerStats");
		menu_list.add("Info");
		menu_list.add("History");
		menu_list.add("Transfer");
		menu_list.add("HistoryStats");
		menu_list.add("LineUp");
		return false;
	}

	@Override
	protected void HandleRequest(Map<String, String> parameters, Response response) {
		long id = Long.valueOf(parameters.get("id"));
		ClubData club = accessor.GetClub(id);
		String command = parameters.getOrDefault("command", "none");
		if (command.equals("update")) {
			int formation_index = Integer.valueOf(parameters.get("formation"));
			club.GetInfo().favorite_formation = Formation.FormationName.values()[formation_index];
			club.WriteInfo();
			Global global = accessor.GetGlobal();
			for (int i = 0; i < 11; ++i) {
				long player_id = Long.valueOf(parameters.getOrDefault(String.format("pos%d", i), "0"));
				global.preferred_lineup[i] = player_id;
			}
			accessor.Flush();
		}
		String tab = parameters.getOrDefault("tab", "default");
		
		CompoundElement body = new CompoundElement();
		response.title = club.GetData().name;

		if (tab.equals("default")) {
			BuildSquad(club, body);
		} else if (tab.equals("fixture")) {
			BuildFutureFixture(club, body);
			BuildPastFixture(club, body);
		} else if (tab.equals("info")) {
			BuildInfo(club, body);
		} else if (tab.equals("history")) {
			BuildHistory(club, body);
		} else if (tab.equals("transfer")) {
			BuildTransfers(club, body);
		} else if (tab.equals("historystats")) {
			BuildHistoryStats(club, body);
		} else if (tab.equals("playerstats")) {
			BuildPlayerStats(club, body);
		} else if (tab.equals("lineup")) {
			BuildLineUp(club, body, parameters);
			BuildNextMatch(club, body);
		}
		response.body = body.ToHtml();
		response.country = club.GetCountry();
		response.league = club.FindDomesticLeague();
	}
	
	private void BuildSquad(ClubData club, CompoundElement body) {
		TableElement table = new TableElement(8, true);
		for (String head : headers) {
			table.AddElement(new TextElement(head));
		}
		List<PlayerData> players = club.GetPlayers();
		players.sort(new PlayerComparator());
		for (PlayerData player : players) {
			PlayerInfo player_info = player.GetInfo();
			PlayerClubInfo club_info = player_info.club_info;
			table.AddElement(new TextElement(club_info != null ? club_info.squad_number : null));
			table.AddElement(LinkedItem.CreatePlayerItem(player));
			table.AddElement(new TextElement(player_info.GetPosition().abbreviate));
			table.AddElement(new TextElement(player.GetAge()));
			table.AddElement(new TextElement(String.format("%d", 
					(int)(PositionAnalyzer.GetOverallScore(player_info) * 100))));
			int injury = FieldAccessor.kZeroBased.Get(player_info.injury);
			if (injury > 0) {
				table.AddElement(new TextElement("Inj " + injury));
			} else {
				table.AddElement(new TextElement(FieldAccessor.kHundredBased.Get(player_info.stamina) + "("
						+ FieldAccessor.kHundredBased.Get(player_info.condition) + ")"));
			}
			table.AddElement(new TextElement(club_info != null ? club_info.contract : null));	
			table.AddElement(new TextElement(club_info != null ? StringUtil.ShortNumber(club_info.wage) : null));
		}
		body.AddElement(table);
	}
	
	private void BuildFutureFixture(ClubData club, CompoundElement body) {
		int week = accessor.GetGlobal().week;
		boolean mid_week = accessor.GetGlobal().mid_week;
		body.AddElement(new TextElement("Future Matches").SetBold(true));
		TableElement table = new TableElement(4, false);
		int match_count = 0;
		while (week < 51 && match_count < 5) {
			if (InsertFutureMatch(club, week, mid_week, table) != null) {
				match_count++;
			}
			if (mid_week) {
				week++;
			}
			mid_week = !mid_week;
		}
		body.AddElement(table);
	}
	
	private void BuildPastFixture(ClubData club, CompoundElement body) {
		int year = accessor.GetGlobal().year;
		int week = accessor.GetGlobal().week;
		boolean mid_week = accessor.GetGlobal().mid_week;
		body.AddElement(new TextElement("Past Matches").SetBold(true));
		TableElement table = new TableElement(4, false);
		while (week >= 0) {
			if (!mid_week) week--;
			mid_week = !mid_week;
			Event event = calendar.GetEvent(week, mid_week);
			if (event == null || event.type != EventType.MATCH) continue;
			for (LeagueData league : club.GetClubParticipatingLeagues()) {
				if (league.GetData().type != event.game_type) continue;
				LeagueInfo league_info = league.GetInfo();
				Schedule schedule = Constant.GetConstant().league_schedule.get(event.game_type);
				int round_id = FieldAccessor.kZeroBased.Get(event.round);
				Match[] matches = schedule.rounds[round_id].matches;
				for (int i = 0; i < matches.length; ++i) {
					Match match = matches[i];
					if (league_info != null && match.host < league_info.team_ids.size() &&
							match.away < league_info.team_ids.size()) {
						ClubData opponent = null;
						String home_or_away = "N";
						boolean reverse_score = false;
						if (league_info.team_ids.get(match.host) == club.GetKey()) {
							opponent = accessor.GetClub(league_info.team_ids.get(match.away));
							if (!match.neutral_site) home_or_away = "H";
						} else if (league_info.team_ids.get(match.away) == club.GetKey()) {
							opponent = accessor.GetClub(league_info.team_ids.get(match.host));
							if (!match.neutral_site) home_or_away = "A";
							reverse_score = true;
						} else continue;
						table.AddElement(LinkedItem.CreateLeagueItem(league));
						table.AddElement(new TextElement(home_or_away));
						table.AddElement(LinkedItem.CreateClubItem(opponent));
						table.AddElement(LinkedItem.CreateMatchItem(
								league.GetMatch(year, round_id, i), reverse_score));
						break;
					}
				}
			}
		}
		body.AddElement(table);
	}
	
	private void BuildInfo(ClubData club, CompoundElement body) {
		ClubInfo info = club.GetInfo();
		TableElement table = new TableElement(2, false);
		table.AddElement(new TextElement("Favorite Formation").SetBold(true));
		table.AddElement(new TextElement(Formation.formations.get(info.favorite_formation).readable_name));
		table.AddElement(new TextElement("Youth Training Level").SetBold(true));
		table.AddElement(new TextElement(FieldAccessor.kZeroBased.Get(info.youth_training_level)));
		table.AddElement(new TextElement("Stadium Size").SetBold(true));
		table.AddElement(new TextElement(StringUtil.Number(info.stadium_size)));
		table.AddElement(new TextElement("Domestic Fans").SetBold(true));
		table.AddElement(new TextElement(StringUtil.ShortNumber(info.domestic_fans)));
		table.AddElement(new TextElement("International Fans").SetBold(true));
		table.AddElement(new TextElement(StringUtil.ShortNumber(info.international_fans)));
		table.AddElement(new TextElement("Finance Balance").SetBold(true));
		table.AddElement(new TextElement(StringUtil.ShortNumber(info.fund)));
		table.AddElement(new TextElement("Estimted Income").SetBold(true));
		table.AddElement(new TextElement(StringUtil.ShortNumber(info.estimated_income)));
		long estimated_cost = 0L;
		for (PlayerData player : club.GetPlayers()) {
			estimated_cost += FieldAccessor.kZeroBased.Get(player.GetInfo().club_info.wage);
		}
		table.AddElement(new TextElement("Estimted Cost").SetBold(true));
		table.AddElement(new TextElement(StringUtil.ShortNumber(estimated_cost)));
		body.AddElement(table);
		body.AddElement(SplitElement.kSplitElement);
		
		table = new TableElement(1, true);
		table.AddElement(new TextElement("Participating Leagues"));
		for (LeagueData league : club.GetClubParticipatingLeagues()) {
			table.AddElement(LinkedItem.CreateLeagueItem(league));
		}
		body.AddElement(table);
	}
	
	private void BuildHistory(ClubData club, CompoundElement body) {
		SortedMap<Integer, ClubRecord> records = club.GetRecords();
		TableElement table = new TableElement(5, true);
		table.AddElement(new TextElement("Year"));
		table.AddElement(new TextElement("Fans"));
		table.AddElement(new TextElement("Income"));
		table.AddElement(new TextElement("Cost"));
		table.AddElement(new TextElement("Balance"));
		for (Entry<Integer, ClubRecord> entry : records.entrySet()) {
			table.AddElement(new TextElement(entry.getKey()));
			table.AddElement(new TextElement(StringUtil.ShortNumber(entry.getValue().domestic_fans) + "/"
					+ StringUtil.ShortNumber(entry.getValue().international_fans)));
			table.AddElement(new TextElement(StringUtil.ShortNumber(entry.getValue().income)));
			table.AddElement(new TextElement(StringUtil.ShortNumber(entry.getValue().expense)));
			table.AddElement(new TextElement(StringUtil.ShortNumber(entry.getValue().balance)));
		}
		body.AddElement(table);
	}
	
	private void BuildTransfers(ClubData club, CompoundElement body) {
		TableElement table = new TableElement(3, true);
		table.AddElement(new TextElement("From"));
		table.AddElement(new TextElement("Player"));
		table.AddElement(new TextElement("To"));
		List<TransferData> transfers = new ArrayList<TransferData>();
		club.GetTransferIn(transfers);
		club.GetTransferOut(transfers);
		transfers.sort(new TransferComparator());
		long income = 0L;
		long cost = 0L;
		int year = 0;
		for (TransferData transfer : transfers) {
			if (transfer.GetYear() != year) {
				if (year > 0) {
					table.AddElement(new TextElement(StringUtil.ShortNumber(income)).SetBold(true));
					table.AddElement(new TextElement(year).SetBold(true));
					table.AddElement(new TextElement(StringUtil.ShortNumber(cost)).SetBold(true));
				}
				year = transfer.GetYear();
				income = 0L;
				cost = 0L;
			}
			if (transfer.GetFromClub().equals(club)) {
				income += transfer.GetFee();
				table.AddElement(new TextElement(StringUtil.ShortNumber(transfer.GetFee())));
				table.AddElement(LinkedItem.CreatePlayerItem(transfer.GetPlayer()));
				table.AddElement(LinkedItem.CreateClubItem(transfer.GetToClub()));
			} else {
				cost += transfer.GetFee();
				table.AddElement(LinkedItem.CreateClubItem(transfer.GetFromClub()));
				table.AddElement(LinkedItem.CreatePlayerItem(transfer.GetPlayer()));
				table.AddElement(new TextElement(StringUtil.ShortNumber(transfer.GetFee())));
			}
		}
		if (year > 0) {
			table.AddElement(new TextElement(StringUtil.ShortNumber(income)).SetBold(true));
			table.AddElement(new TextElement(year).SetBold(true));
			table.AddElement(new TextElement(StringUtil.ShortNumber(cost)).SetBold(true));
		}
		body.AddElement(table);
	}
	
	private void BuildHistoryStats(ClubData club, CompoundElement body) {
		TableElement table = new TableElement(3, true);
		table.AddElement(new TextElement("Year"));
		table.AddElement(new TextElement("League"));
		table.AddElement(new TextElement("Rank"));
		SortedMap<Integer, ClubRecord> records = club.GetRecords();
		SortedSet<LeagueData> domestic_leagues = new TreeSet<LeagueData>();
		SortedSet<LeagueData> cups = new TreeSet<LeagueData>();
		for (LeagueData league : accessor.GetAllLeauges()) {
			if (club.GetCountry().equals(league.GetCountry()) && league.GetData().type == Type.DOMESTIC_LEAGUE) {
				domestic_leagues.add(league);
				continue;
			}
			if (league.GetCountry() != null && !club.GetCountry().equals(league.GetCountry())) continue;
			cups.add(league);
		}
		body.AddElement(new TextElement("League History").SetBold(true));
		body.AddElement(PopulateLeagueResults(club, domestic_leagues, records.keySet(), true));
		body.AddElement(PopulateLeagueResults(club, cups, records.keySet(), false));
	}
	
	private void BuildPlayerStats(ClubData club, CompoundElement body) {
		TableElement table = new TableElement(21, true);
		PlayerHandler.AddHeaderToTable("Player", table);
		List<PlayerData> players = club.GetPlayers();
		players.sort(new PlayerComparator());
		Map<Long, PlayerLeagueRecord> player_stats =
				LeagueUtil.ComputePlayerStats(club.GetMatches(accessor.GetGlobal().year));
		final PlayerLeagueRecord empty_record = new PlayerLeagueRecord();
		for (PlayerData player : players) {
			PlayerLeagueRecord record = player_stats.getOrDefault(player.GetKey(), empty_record);
			TextElement row_title = LinkedItem.CreatePlayerItem(player);
			row_title.content = row_title.content + " (" + player.GetInfo().club_info.squad_number + " " +
					player.GetPosition().abbreviate + ")";
			PlayerHandler.AddStatsToTable(row_title, record, table);
		}
		body.AddElement(table);
	}
	
	private void BuildLineUp(ClubData club, CompoundElement body, Map<String, String> parameters) {
		if (!club.equals(accessor.GetControlledClub())) {
			body.AddElement(new TextElement("This club is not controllable."));
			body.AddElement(SplitElement.kSplitElement);
			return;
		}
		Global global = accessor.GetGlobal();
		FormElement form = new FormElement();
		form.AddElement(new TextElement("Formation"));
		TreeMap<String, String> formations = new TreeMap<String, String>();
		for (Formation f : Formation.formations.values()) {
			formations.put(f.readable_name, String.valueOf(f.name.ordinal()));
		}
		SelectElement formation = new SelectElement("formation", formations);
		formation.SetDefault(String.valueOf(club.GetInfo().favorite_formation.ordinal()));
		form.AddElement(formation);
		form.AddElement(SplitElement.kSplitElement);
		List<Position> positions = FormationUtil.GetFormationPositions(club.GetInfo().GetFavoriteFormation());
		TableElement table = new TableElement(2, false);
		TreeMap<String, String> players = new TreeMap<String, String>();
		players.put("   [Auto Select]   ", "0");
		for (PlayerData player : club.GetPlayers()) {
			String player_view = String.format("%2d-[%s] %s (%02d)", player.GetInfo().club_info.squad_number,
					player.GetPosition().abbreviate, player.GetData().name,
					FieldAccessor.kHundredBased.Get(player.GetInfo().stamina));
			players.put(player_view, String.valueOf(player.GetKey()));
		}
		for (int i = 0; i < 11; ++i) {
			table.AddElement(new TextElement(positions.get(i).abbreviate));
			SelectElement player_option = new SelectElement(String.format("pos%d", i), players);
			player_option.SetDefault(String.valueOf(global.preferred_lineup[i]));
			table.AddElement(player_option);
		}
		form.AddElement(table);
		form.AddElement(new ButtonElement("Update",
				ConstructLink("/club", parameters, "command", "update")));
		body.AddElement(form);
	}
	
	private void BuildNextMatch(ClubData club, CompoundElement body) {
		Global global = accessor.GetGlobal();
		TableElement table = new TableElement(4, false);
		LeagueData league = InsertFutureMatch(club, global.week, global.mid_week, table);
		if (league == null) return;
		body.AddElement(new TextElement("Next Match: ").SetBold(true));
		body.AddElement(table);
		body.AddElement(new TextElement("Unavailable Player: ").SetBold(true));
		List<PlayerData> unavailable = engine.Controller().GetUnavailablePlayers(club, league);
		for (PlayerData player: unavailable) {
		  body.AddElement(LinkedItem.CreatePlayerItem(player));
		}
	}
	
	private LeagueData InsertFutureMatch(ClubData club, int week, boolean mid_week, TableElement table) {
		Event event = calendar.GetEvent(week, mid_week);
		if (event == null || event.type != EventType.MATCH) return null;
		for (LeagueData league : club.GetClubParticipatingLeagues()) {
			if (league.GetData().type != event.game_type) continue;
			LeagueInfo league_info = league.GetInfo();
			Schedule schedule = Constant.GetConstant().league_schedule.get(event.game_type);
			for (Match match : schedule.rounds[FieldAccessor.kZeroBased.Get(event.round)].matches) {
				if (league_info != null && match.host < league_info.team_ids.size() &&
						match.away < league_info.team_ids.size()) {
					ClubData opponent = null;
					String home_or_away = "N";
					if (league_info.team_ids.get(match.host) == club.GetKey()) {
						opponent = accessor.GetClub(league_info.team_ids.get(match.away));
						if (!match.neutral_site) home_or_away = "H";
					} else if (league_info.team_ids.get(match.away) == club.GetKey()) {
						opponent = accessor.GetClub(league_info.team_ids.get(match.host));
						if (!match.neutral_site) home_or_away = "A";
					} else continue;
					table.AddElement(new TextElement(StringUtil.GetTime(week, mid_week)));
					table.AddElement(LinkedItem.CreateLeagueItem(league));
					table.AddElement(new TextElement(home_or_away));
					table.AddElement(LinkedItem.CreateClubItem(opponent));
					return league;
				}
			}
		}
		return null;
	}
	
	private TableElement PopulateLeagueResults(
			ClubData club, Collection<LeagueData> leagues, Collection<Integer> years, boolean sort_by_year) {
		TableElement table = new TableElement(3, true);
		table.AddElement(new TextElement("Year"));
		table.AddElement(new TextElement("League"));
		table.AddElement(new TextElement("Rank"));
		List<LeagueData> dummy_leagues = new ArrayList<LeagueData>();
		dummy_leagues.add(null);
		for (LeagueData league0 : sort_by_year ? dummy_leagues : leagues) {
			for (Integer year : years) {
				for (LeagueData league1 : sort_by_year ? leagues : dummy_leagues) {
					LeagueData league = sort_by_year ? league1 : league0;
					LeagueRecordData stats = league.GetLeagueRecord(year);
					if (!stats.Started()) continue;
					int rank = stats.GetClubRank(club);
					if (rank <= 0) continue;
					table.AddElement(new TextElement(year));
					TextElement league_text = LinkedItem.CreateLeagueItem(league, year);
					league_text.content = league.GetData().name;
					table.AddElement(league_text);
					table.AddElement(new TextElement(rank));
				}
			}
		}
		return table;
	}
	
	private static class PlayerComparator implements Comparator<PlayerData> {
		@Override
		public int compare(PlayerData o1, PlayerData o2) {
			int ret = o1.GetInfo().GetPosition().name.ordinal() - o2.GetInfo().GetPosition().name.ordinal();
			if (ret != 0) return ret;
			int wage1 = FieldAccessor.kZeroBased.Get(o1.GetInfo().club_info.wage);
			int wage2 = FieldAccessor.kZeroBased.Get(o2.GetInfo().club_info.wage);
			if (wage1 > wage2) return -1;
			if (wage1 < wage2) return 1;
			return 0;
		}
	}
	
	private static class TransferComparator implements Comparator<TransferData> {

		@Override
		public int compare(TransferData o1, TransferData o2) {
			if (o1.GetYear() < o2.GetYear()) return -1;
			if (o1.GetYear() > o2.GetYear()) return 1;
			return 0;
		}
	}

	private static String[] headers = { "#", "Name", "Pos", "Age", "Abi", "Con", "Ct", "Wage" };
}
