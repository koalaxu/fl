package fl.webui.handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import fl.data.PlayerHistory.PlayerRecord;
import fl.data.Position;
import fl.engine.data.ClubData;
import fl.engine.data.PlayerData;
import fl.engine.utils.PositionAnalyzer;
import fl.utils.ScoredObject;
import fl.utils.TopN;
import fl.webui.utils.CompoundElement;
import fl.webui.utils.LinkedItem;
import fl.webui.utils.TableElement;
import fl.webui.utils.TextElement;

public class PlayersHandler extends BaseHandler {
	public PlayersHandler() {
		if (kAbbreviateToPositions == null) {
			kAbbreviateToPositions = new HashMap<String, Position>();
			for (Position pos : Position.positions.values()) {
				kAbbreviateToPositions.put(pos.abbreviate, pos);
			}
		}
	}

	@Override
	protected boolean GetMenu(List<String> menu_list) {
		for (Position pos : Position.positions.values()) {
			menu_list.add(pos.abbreviate);
		}
		return true;
	}
	
	@Override
	protected void HandleRequest(Map<String, String> parameters, Response response) {
		int year = Integer.valueOf(parameters.getOrDefault("year",
				Integer.valueOf(accessor.GetGlobal().year).toString()));
		String tab = parameters.getOrDefault("tab", "gk").toUpperCase();
		response.title = "All Players ( " + tab + " )";
		CompoundElement body = new CompoundElement();
		Position position = kAbbreviateToPositions.getOrDefault(tab, Position.GoalKeeper);
		if (year == accessor.GetGlobal().year) {
			PopulateCurrentYear(position, body);
		} else {
			PopulateYear(position, year, body);
		}
		response.body = body.ToHtml();
	}
	
	private void PopulateCurrentYear(Position position, CompoundElement body) {
		List<ScoredObject<PlayerData>> players = new ArrayList<ScoredObject<PlayerData>>();
		for (PlayerData player: accessor.GetAllActivePlayers()) {
			if (player.GetInfo().GetPosition().equals(position)) {
				players.add(new ScoredObject<PlayerData>(player,
						PositionAnalyzer.GetOverallScore(player.GetInfo(), position)));
			}
		}
		PopulateTopN(players, body, PlayerData::GetClub, 0);
	}
	
	private void PopulateYear(Position position, int year, CompoundElement body) {
		List<ScoredObject<PlayerData>> players = new ArrayList<ScoredObject<PlayerData>>();
		for (PlayerData player: accessor.GetAllPlayers()) {
			if (player.GetPosition().equals(position)) {
				PlayerRecord record = player.GetAnnualRecord(year);
				if (record == null) continue;
				players.add(new ScoredObject<PlayerData>(player, record.score));
			}
		}
		PopulateTopN(players, body, player -> {  return accessor.GetClub(player.GetAnnualRecord(year).club_id);  },
				accessor.GetGlobal().year - year);
	}
	
	private void PopulateTopN(List<ScoredObject<PlayerData>> players, CompoundElement body,
			Function<PlayerData, ClubData> club_getter, int years_ago) {
		final int kTopPlayers = 200;
		players = TopN.FindSortedTopN(players, kTopPlayers, ScoredObject.SortByScore());
		TableElement table = new TableElement(4, true);
		for (String header : kHeaders) table.AddElement(new TextElement(header));
		for (ScoredObject<PlayerData> player : players) {
			table.AddElement(LinkedItem.CreatePlayerItem(player.element));
			table.AddElement(LinkedItem.CreateClubItem(club_getter.apply(player.element)));
			table.AddElement(new TextElement(player.element.GetAge() - years_ago));
			table.AddElement(new TextElement((int)(player.score * 100)));
		}
		body.AddElement(table);
	}

	private static Map<String, Position> kAbbreviateToPositions;
	private static String[] kHeaders = { "Name", "Club", "Age", "Ability" };
}
