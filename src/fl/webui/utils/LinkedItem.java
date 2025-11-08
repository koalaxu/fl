package fl.webui.utils;

import fl.data.Match;
import fl.data.MatchResult;
import fl.engine.data.ClubData;
import fl.engine.data.CountryData;
import fl.engine.data.LeagueData;
import fl.engine.data.MatchData;
import fl.engine.data.PlayerData;

public class LinkedItem {
	public static TextElement CreateCountryItem(CountryData country) {
		TextElement text = new TextElement(country.GetData().name);
		text.link = "/country?id=" + country.GetKey();
		return text;
	}
	public static TextElement CreateLeagueItem(LeagueData league) {
		TextElement text = new TextElement(league.GetData().name);
		text.link = "/league?id=" + league.GetKey();
		return text;
	}
	public static TextElement CreateLeagueItem(LeagueData league, int year) {
		TextElement text = new TextElement(league.GetData().name + " (" + year + ")");
		text.link = "/league?id=" + league.GetKey() + "&year=" + year;
		return text;
	}
	public static TextElement CreateClubItem(ClubData club) {
		TextElement text = new TextElement(club.GetData().name);
		text.link = "/club?id=" + club.GetKey();
		return text;
	}
	public static TextElement CreatePlayerItem(PlayerData player) {
		TextElement text = new TextElement(player.GetData().name);
		text.link = "/player?id=" + player.GetKey();
		return text;
	}
	public static TextElement CreateMatchItem(MatchData match) {
		return CreateMatchItem(match, false);
	}
	public static TextElement CreateMatchItem(MatchData match, boolean reverse_result) {
		TextElement text = new TextElement(PopluateScore(match.GetResult(), reverse_result));
		Match m = match.GetMatch();
		text.link = "/match?year=" + m.year + "&lid=" + m.league_id + "&rid=" + m.round_id + "&mid=" + m.match_id;
		return text;
	}
	
	private static String PopluateScore(MatchResult result, boolean reverse_result) {
		String str = reverse_result ? 
				result.away.stats.goals + " : " + result.home.stats.goals :
				result.home.stats.goals + " : " + result.away.stats.goals;
		if (result.home.stats.shootout_goals + result.away.stats.shootout_goals > 0) {
			str += "\n( " + (reverse_result ? 
					result.away.stats.shootout_goals + " : " + result.home.stats.shootout_goals :
						result.home.stats.shootout_goals + " : " + result.away.stats.shootout_goals)
					+ " )";
		} else if (result.extra_time) {
			str += " [Ex]";
		}
		return str;
	}
}
