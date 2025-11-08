package fl.webui.handlers;

import java.util.ArrayList;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;

import fl.data.Formation;
import fl.data.MatchResult;
import fl.data.MatchResult.Event;
import fl.data.MatchResult.TeamData;
import fl.data.MatchStats;
import fl.data.Position;
import fl.engine.data.ClubData;
import fl.engine.data.MatchData;
import fl.engine.data.PlayerData;
import fl.engine.utils.FormationUtil;
import fl.engine.utils.MatchStatsUtil;
import fl.utils.StringUtil;
import fl.webui.utils.BaseElement;
import fl.webui.utils.ButtonElement;
import fl.webui.utils.CompoundElement;
import fl.webui.utils.LinkedItem;
import fl.webui.utils.TableElement;
import fl.webui.utils.TableElement.Cell.Alignment;
import fl.webui.utils.TextElement;

public class MatchHandler extends BaseHandler {
	@Override
	protected boolean GetMenu(List<String> menu_list) {
		menu_list.add("Overview");
		menu_list.add("Timeline");
		menu_list.add("PlayerStats");
		menu_list.add("Commentary");
		return false;
	}

	@Override
	protected void HandleRequest(Map<String, String> parameters, Response response) {
		long year = Long.valueOf(parameters.getOrDefault("year",
				Long.valueOf(accessor.GetGlobal().year).toString()));
		long lid = Long.valueOf(parameters.get("lid"));
		int rid = Integer.valueOf(parameters.get("rid"));
		int mid = Integer.valueOf(parameters.get("mid"));
		String tab = parameters.getOrDefault("tab", "default");
		MatchData match = accessor.GetMatch(lid, rid, mid, year);
		
		if (parameters.getOrDefault("command", "none").equals("archive")) {
			match.ArchiveCommentary();
			accessor.Flush();
			System.out.println("Commentary archived for " + match.GetLeague().GetData().name + " - Round " +
					match.GetMatch().round_id + " | Match " + match.GetMatch().match_id + ". ");
		}
		
		response.title = year + " " + match.GetLeague().GetData().name + " - Round " + rid;
		CompoundElement body = new CompoundElement();
		if (tab.equals("default")) {
			BuildOverview(match, body);
		} else if (tab.equals("timeline")) {
			BuildTimeline(match, body);
		} else if (tab.equals("playerstats")) {
			BuildPlayerStats(match, body);
		} else if (tab.equals("commentary")) {
			BuildCommentary(match, parameters, body);
		}
		
		response.body = body.ToHtml();
		response.league = match.GetLeague();
		response.country = match.GetLeague().GetCountry();
		response.year = (int) year;
	}
	
	private void BuildOverview(MatchData match, CompoundElement body) {
		MatchResult result = match.GetResult();
		MatchStats home_stats = MatchStatsUtil.AggregateTeamStats(result.home);
		MatchStats away_stats = MatchStatsUtil.AggregateTeamStats(result.away);
		TableElement table = new TableElement(3, true);
		table.SetDefaultAlignment(Alignment.CENTER);
		table.AddElement(LinkedItem.CreateClubItem(match.GetHomeClub()));
		table.AddElement(new TextElement(result.extra_time ? "Extra Time" : "Full Time"));
		table.AddElement(LinkedItem.CreateClubItem(match.GetAwayClub()));
		table.AddElement(new TextElement(result.home.stats.goals));
		table.AddElement(new TextElement("Goals"));
		table.AddElement(new TextElement(result.away.stats.goals));
		if (result.home.stats.shootout_goals + result.away.stats.shootout_goals > 0) {
			table.AddElement(new TextElement(result.home.stats.shootout_goals));
			table.AddElement(new TextElement("PK"));
			table.AddElement(new TextElement(result.away.stats.shootout_goals));
		}
		long total_control_time = result.home.stats.control_time + result.away.stats.control_time;
		table.AddElement(new TextElement(StringUtil.PercentageNumber(
				result.home.stats.control_time, total_control_time)));
		table.AddElement(new TextElement("Possession"));
		table.AddElement(new TextElement(StringUtil.PercentageNumber(
				result.away.stats.control_time, total_control_time)));
		table.AddElement(new TextElement(home_stats.shot));
		table.AddElement(new TextElement("Shots"));
		table.AddElement(new TextElement(away_stats.shot));
		table.AddElement(new TextElement(home_stats.shot_ontarget));
		table.AddElement(new TextElement("Shot on Target"));
		table.AddElement(new TextElement(away_stats.shot_ontarget));
		table.AddElement(new TextElement(home_stats.pass));
		table.AddElement(new TextElement("Pass"));
		table.AddElement(new TextElement(away_stats.pass));
		table.AddElement(new TextElement(StringUtil.PercentageNumber(home_stats.pass_succeed, home_stats.pass)));
		table.AddElement(new TextElement("Pass Success%"));
		table.AddElement(new TextElement(StringUtil.PercentageNumber(away_stats.pass_succeed, away_stats.pass)));
		table.AddElement(new TextElement(home_stats.yellow));
		table.AddElement(new TextElement("Yellow Card"));
		table.AddElement(new TextElement(away_stats.yellow));
		table.AddElement(new TextElement(home_stats.red));
		table.AddElement(new TextElement("Red Card"));
		table.AddElement(new TextElement(away_stats.red));
		table.AddElement(new TextElement(result.home.stats.corner_kick));
		table.AddElement(new TextElement("Corner Kick"));
		table.AddElement(new TextElement(result.away.stats.corner_kick));
		table.AddElement(new TextElement(result.home.stats.free_kick));
		table.AddElement(new TextElement("Free Kick"));
		table.AddElement(new TextElement(result.away.stats.free_kick));
		table.AddElement(new TextElement(result.home.stats.penalty_kick));
		table.AddElement(new TextElement("Penalty Kick"));
		table.AddElement(new TextElement(result.away.stats.penalty_kick));
		if (result.home.stats.abandoned) {
			table.AddElement(new TextElement("Canceled 0 : 3"));
		} else if (result.away.stats.abandoned) {
			table.AddElement(new TextElement("Canceled 3 : 0"));
		}
		body.AddElement(table);
		
		for (int i = 0; i < 2; ++i) {
			ClubData club = i == 0 ? match.GetHomeClub() : match.GetAwayClub();
			TeamData team_result = i == 0 ? match.GetResult().home : match.GetResult().away;
			Formation formation = Formation.formations.get(team_result.formation);
			TextElement title = new TextElement(club.GetData().name + " (" + kSides[i] + ") - Lineup (" +
					formation.readable_name + ")").SetTitle(true);
			
			TableElement lineup = new TableElement(5, false);
			int index = i == 0 ? 0 : 10;
			for (int j = 0; j < 4; ++j) {
				int r = i == 0 ? j : 3 - j;
				int[] line = kFormationPositions[0];
				if (r == 1) line = kFormationPositions[formation.NumberOfDefenders() - 1];
				else if (r == 2) line = kFormationPositions[formation.NumberOfMidFielders() - 1];
				else if (r == 3) line = kFormationPositions[formation.NumberOfAttackers() - 1];
				Stack<BaseElement> stack = new Stack<BaseElement> ();
				for (int k = 0; k < 5; ++k) {
					final TextElement empty_text = new TextElement("");
					if (line[k] == 0) stack.push(empty_text);
					else {
						stack.push(LinkedItem.CreatePlayerItem(
								accessor.GetPlayer(team_result.lineup[index])));
						if (i == 0) index++; else index--;
					}
				}
				while (!stack.isEmpty()) {
					lineup.AddElement(stack.pop());
				}
			}
			body.AddElement(title);
			body.AddElement(lineup);
		}
	}
	
	private void BuildTimeline(MatchData match, CompoundElement body) {
		MatchResult result = match.GetResult();
		TableElement table = new TableElement(3, true);
		table.SetDefaultAlignment(Alignment.CENTER);
		table.AddElement(LinkedItem.CreateClubItem(match.GetHomeClub()));
		table.AddElement(new TextElement(result.home.stats.goals + " : " + result.away.stats.goals));
		table.AddElement(LinkedItem.CreateClubItem(match.GetAwayClub()));
		List<Event> home_events = result.home.events;
		List<Event> away_events = result.away.events;
		for (int i = 0, j = 0; i < home_events.size() || j < away_events.size();) {
			boolean use_home = j >= away_events.size() || ( i < home_events.size() &&
					home_events.get(i).time < away_events.get(j).time);
			Event event = use_home ? home_events.get(i++) : away_events.get(j++);
			TextElement event_text = new TextElement(GenerateEventText(event));
			final TextElement empty_text = new TextElement("");
			table.AddElement(use_home ? event_text : empty_text);
			table.AddElement(new TextElement(StringUtil.GameTime(event.time)));
			table.AddElement(use_home ? empty_text : event_text);
		}
		body.AddElement(table);
	}
	
	private void BuildPlayerStats(MatchData match, CompoundElement body) {
		for (int i = 0; i < 2; ++i) {
			ClubData club = i == 0 ? match.GetHomeClub() : match.GetAwayClub();
			TeamData result = i == 0 ? match.GetResult().home : match.GetResult().away;
			TextElement title = new TextElement(club.GetData().name + " (" + kSides[i] + ")");
			title.title = true;
			body.AddElement(title);
			List<Long> player_ids = new ArrayList<Long>();
			List<Long> sub_player_ids = new ArrayList<Long>();
			for (int j = 0; j < 11; ++j) {
				player_ids.add(result.lineup[j]);
			}
			for (Entry<Long, MatchStats> e : result.player_stats.entrySet()) {
				if (player_ids.contains(e.getKey())) continue;
				sub_player_ids.add(e.getKey());
			}
			sub_player_ids.sort(new Comparator<Long>() {
				@Override
				public int compare(Long o1, Long o2) {
					return result.player_stats.get(o1).start_time - result.player_stats.get(o2).start_time;
				}
			});
			player_ids.addAll(sub_player_ids);
			TableElement[] tables = new TableElement[3];
			tables[0] = new TableElement(10, true);
			tables[1] = new TableElement(10, true);
			tables[2] = new TableElement(9, true);
			for (String head : kOverallHeaders) tables[0].AddElement(new TextElement(head));
			for (String head : kAttackHeaders) tables[1].AddElement(new TextElement(head));
			for (String head : kDefendHeaders) tables[2].AddElement(new TextElement(head));
			
			List<Position> positions =
					FormationUtil.GetFormationPositions(Formation.formations.get(result.formation));
			int index = 0;
			double score_adjustment = MatchStatsUtil.GetScoreAdjustment(match.GetResult(), i == 0);
			for (Long player_id : player_ids) {
				PlayerData player = accessor.GetPlayer(player_id);
				MatchStats stats = result.player_stats.get(player_id);
				for (TableElement table : tables) {
					table.AddElement(LinkedItem.CreatePlayerItem(player));
					if (index < 11) {
						table.AddElement(new TextElement(positions.get(index).abbreviate));
					} else {
						table.AddElement(new TextElement(player.GetPosition().abbreviate));
					}
				}
				tables[0].AddElement(new TextElement(stats.start_time / 60));
				tables[0].AddElement(new TextElement((stats.end_time - stats.start_time - 1) / 60 + 1));
				tables[0].AddElement(new TextElement(stats.goal));
				tables[0].AddElement(new TextElement(stats.assistance));
				tables[0].AddElement(new TextElement(stats.goal_conceded));
				tables[0].AddElement(new TextElement(stats.yellow));
				tables[0].AddElement(new TextElement(stats.red));
				tables[0].AddElement(new TextElement(String.format("%.1f",
						(float)MatchStatsUtil.GetScore(stats, score_adjustment) / 10.0)));
				tables[1].AddElement(new TextElement(stats.key_pass));
				tables[1].AddElement(new TextElement(stats.pass_succeed));
				tables[1].AddElement(new TextElement(stats.pass));
				tables[1].AddElement(new TextElement(stats.dribble_succeed));
				tables[1].AddElement(new TextElement(stats.dribble));
				tables[1].AddElement(new TextElement(stats.shot_ontarget));
				tables[1].AddElement(new TextElement(stats.shot));
				tables[1].AddElement(new TextElement(stats.fouled));
				tables[2].AddElement(new TextElement(stats.header_succeed));
				tables[2].AddElement(new TextElement(stats.header));
				tables[2].AddElement(new TextElement(stats.clearance));
				tables[2].AddElement(new TextElement(stats.intecept));
				tables[2].AddElement(new TextElement(stats.tackle));
				tables[2].AddElement(new TextElement(stats.save));
				tables[2].AddElement(new TextElement(stats.foul));
				index++;
			}
			body.AddElement(tables[0]);
			body.AddElement(tables[1]);
			body.AddElement(tables[2]);
		}
	}
	
	private void BuildCommentary(MatchData match, Map<String, String> parameters, CompoundElement body) {
		String content = match.GetCommentary();
		if (content == null) {
			body.AddElement(new TextElement("No commentary"));
			return;
		}
		
		TextElement text = new TextElement(content);
		body.AddElement(new ButtonElement("Archive", ConstructLink("/match", parameters, "command", "archive")));
		body.AddElement(text);
		
	}
	
	private String GenerateEventText(Event event) {
		PlayerData player1 = accessor.GetPlayer(event.player1);
		PlayerData player2 = event.player2 > 0 ? accessor.GetPlayer(event.player2) : null;
		switch (event.type) {
		case GOAL:
			return player1.GetData().name + " ⚽️" +
				(player2 != null ? "  ( " + player2.GetData().name + " 👟 )" : "");
		case PENALTY_GOAL:
			return player1.GetData().name + " ⚽ (P)";
		case RED:
			return player1.GetData().name + " 🟥";
		case SUBSTITUTE:
			return player1.GetData().name + " 🔄 " + player2.GetData().name;
		case YELLOW:
			return player1.GetData().name + " 🟨";
		}
		return null;
	}
	
	private static String[] kSides = { "Home", "Away" };
	private static String[] kOverallHeaders = { "Name", "Pos", "St", "Min", "G", "A", "C", "Y", "R", "Score" };
	private static String[] kAttackHeaders = { "Name", "Pos", "KP", "SP", "P", "SD", "D", "SO", "S", "Fd" };
	private static String[] kDefendHeaders = { "Name", "Pos", "SH", "H", "C", "I", "T", "S", "F" };
	private int[][] kFormationPositions = { { 0, 0, 1, 0, 0 }, { 0, 1, 0, 1, 0 }, { 0, 1, 1, 1, 0 },
			{ 1, 1, 0, 1, 1 }, { 1, 1, 1, 1, 1 } };
}
