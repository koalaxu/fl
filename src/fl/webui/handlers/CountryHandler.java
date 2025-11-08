package fl.webui.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import fl.engine.data.ClubData;
import fl.engine.data.CountryData;
import fl.engine.data.LeagueData;
import fl.engine.data.PlayerData;
import fl.engine.utils.ContinentalLeagueUtil;
import fl.engine.utils.PositionAnalyzer;
import fl.utils.ScoredObject;
import fl.utils.StringUtil;
import fl.webui.utils.CompoundElement;
import fl.webui.utils.LinkedItem;
import fl.webui.utils.SplitElement;
import fl.webui.utils.TableElement;
import fl.webui.utils.TextElement;

public class CountryHandler extends BaseHandler {
	@Override
	protected boolean GetMenu(List<String> menu_list) {
		menu_list.add("League");
		menu_list.add("Club");
		return false;
	}


	@Override
	protected void HandleRequest(Map<String, String> parameters, Response response) {
		long id = Long.valueOf(parameters.get("id"));
		String tab = parameters.getOrDefault("tab", "default");
		CompoundElement body = new CompoundElement();
		CountryData country = accessor.GetCountry(id);
		if (tab.equals("default")) {
			BuildLeagues(country, body);
		} else if (tab.equals("club")) {
			BuildClubs(country, body);
		}

		response.title = country.GetData().name;
		response.body = body.ToHtml();	
	}
	
	private void BuildLeagues(CountryData country, CompoundElement body) {
		TableElement table = new TableElement(1, false);
		List<LeagueData> leagues = country.GetLeagues();
		for (LeagueData league : leagues) {
			table.AddElement(LinkedItem.CreateLeagueItem(league));
		}
		body.AddElement(table);
		body.AddElement(SplitElement.kSplitElement);
		int rank = accessor.GetGlobal().country_ranks[(int) (country.GetKey() - 1)];
		body.AddElement(new TextElement("Rank: " + rank));
		body.AddElement(new TextElement("Champion League Clubs: " +
				ContinentalLeagueUtil.kContinentalLeagueQuotas[0][rank - 1]));
		body.AddElement(new TextElement("Euro League Clubs: " +
				ContinentalLeagueUtil.kContinentalLeagueQuotas[1][rank - 1]));
		
	}
	
	private void BuildClubs(CountryData country, CompoundElement body) {
		TableElement table = new TableElement(kHeader.length, true);
		for (String header : kHeader) table.AddElement(new TextElement(header));
		for (ClubData club : country.GetClubs()) {
			table.AddElement(LinkedItem.CreateClubItem(club));
			table.AddElement(new TextElement(StringUtil.ShortNumber(club.GetInfo().stadium_size)));
			table.AddElement(new TextElement(StringUtil.ShortNumber(club.GetInfo().domestic_fans) + "/"
					+ StringUtil.ShortNumber(club.GetInfo().international_fans)));
			List<PlayerData> players = club.GetPlayers();
			table.AddElement(new TextElement(players.size()));
			List<ScoredObject<PlayerData>> scored_players = new ArrayList<ScoredObject<PlayerData>>();
			long total_wage = 0L;
			for (PlayerData player : players) {
				scored_players.add(new ScoredObject<PlayerData>(player,
						PositionAnalyzer.GetOverallScore(player.GetInfo())));
				total_wage += player.GetInfo().club_info.wage;
			}
			scored_players.sort(ScoredObject.SortByScore());
			double total_score = 0;
			double total_weight = 0;
			for (int i = 0; i < 22 && i < scored_players.size(); ++i) {
				if (i < 11) {
					total_score += scored_players.get(i).score;
					total_weight += 1;
				} else {
					total_score += scored_players.get(i).score * 0.5;
					total_weight += 0.5;
				}
			}
			table.AddElement(new TextElement((int)(total_score * 100 / total_weight)));
			table.AddElement(new TextElement(StringUtil.ShortNumber(total_wage)));
		}
		body.AddElement(table);
	}


	private static String[] kHeader = { "Club", "Stadium", "Fans", "Players", "Score", "Wage Cost" };
}
