package fl.engine.utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fl.data.LeagueInfo;
import fl.engine.data.ClubData;
import fl.engine.data.CountryData;
import fl.engine.data.DataAccessor.Collection;
import fl.engine.data.LeagueData;
import fl.engine.data.MatchData;
import fl.utils.ScoredObject;

public class ContinentalLeagueUtil {
	public ContinentalLeagueUtil(int[] country_ranks) {
		this.country_ranks = country_ranks;
		country_rank_indices = new int[country_ranks.length + 1];
		for (int i = 0; i < country_ranks.length; ++i) {
			country_rank_indices[country_ranks[i]] = i;
		}
	}
	
	public int GetCountryRank(int country_id) {
		return country_rank_indices[country_id];
	}
	
	public void FindContinentalLeagueQualifiedTeams(
			CountryData country, List<ClubData> ranked_teams, ClubData champion_winner, ClubData euro_winner,
			ClubData cup_winner, List<ClubData> champion_qualified, List<ClubData> euro_qualified) {
		champion_qualified.clear();
		euro_qualified.clear();
		int country_rank = GetCountryRank((int) country.GetKey());
		int champion_quota = kContinentalLeagueQuotas[0][country_rank];
		int euro_quota = kContinentalLeagueQuotas[1][country_rank];
		Set<ClubData> selected_teams = new HashSet<ClubData>();
		if (champion_winner.GetCountry().equals(country)) {
			champion_qualified.add(champion_winner);
			selected_teams.add(champion_winner);
		}
		if (euro_winner.GetCountry().equals(country)) {
			champion_qualified.add(euro_winner);
			selected_teams.add(euro_winner);
		}
		int index = 0;
		while (champion_qualified.size() < champion_quota) {
			ClubData candidate = ranked_teams.get(index++);
			if (selected_teams.contains(candidate)) continue;
			champion_qualified.add(candidate);
			selected_teams.add(candidate);
		}
		if (!selected_teams.contains(cup_winner)) {
			euro_qualified.add(cup_winner);
			selected_teams.add(cup_winner);
		}
		while (euro_qualified.size() < euro_quota) {
			ClubData candidate = ranked_teams.get(index++);
			if (selected_teams.contains(candidate)) continue;
			euro_qualified.add(candidate);
		}
	}
	
	public long[] RerankQualifiedTeams(List<ClubData> teams,
			List<ClubData> ranked_league1_clubs, List<ClubData> ranked_league2_clubs) {
		ClubComparator comp = new ClubComparator(ranked_league1_clubs, ranked_league2_clubs);
		teams.sort(comp);
		long[] ret = new long[teams.size()];
		for (int i = 0; i < ret.length; ++i) {
			ret[i] = teams.get(i).GetKey();
		}
		return ret;
	}
	
	public void AssignChampionLeagueTeams(long[][] country_league_leaders, LeagueInfo league) {
		league.qualified_teams = new ArrayList<Long>();
		for (int i = 0; i < 8; ++i) {
			int country_rank = i % 5;
			int country_index = country_ranks[country_rank];
			int per_country_index = i / 5;
			league.qualified_teams.add(country_league_leaders[country_index - 1][per_country_index]);
		}
		for (int i = 8; i < 32; ++i) {
			int country_rank = (i - 8) % 12;
			int country_index = country_ranks[country_rank];
			int per_country_index = (i - 8) / 12 + (country_rank < 3 ? 2 : (country_rank < 5 ? 1 : 0));
			league.qualified_teams.add(country_league_leaders[country_index - 1][per_country_index]);
		}
	}
	
	public void AssignEuroLeagueTeams(long[][] country_league_leaders, LeagueInfo league) {
		league.qualified_teams = new ArrayList<Long>();
		for (int i = 0; i < 32; ++i) {
			league.qualified_teams.add(country_league_leaders[country_ranks[i % 12] - 1][i / 12]);
		}
	}
	
	public static int[] ComputeCountryScore(int year, Collection<CountryData> all_countries,
			LeagueData champion_league, LeagueData euro_league) {
		Map<CountryData, Double> country_scores = new HashMap<CountryData, Double>();
		Map<CountryData, Set<ClubData>> teams = new HashMap<CountryData, Set<ClubData>>();
		for (CountryData country : all_countries) {
			country_scores.put(country, 0.0);
			teams.put(country, new HashSet<ClubData>());
		}
		if (!ComputeCountryScoreForOneLeague(year, champion_league, country_scores, teams)) return null;
		if (!ComputeCountryScoreForOneLeague(year, euro_league, country_scores, teams)) return null;
		for (CountryData country : all_countries) {
			country_scores.put(country, country_scores.get(country) / teams.get(country).size());
		}
		List<ScoredObject<CountryData>> ranked_countries = ScoredObject.MapToScoredList(country_scores);
		ranked_countries.sort(ScoredObject.SortByScore());
		int[] ret = new int[ranked_countries.size()];
		for (int i = 0; i < ret.length; ++i) {
			ret[i] = (int) ranked_countries.get(i).element.GetKey();
		}
		return ret;
	}
	
	private static boolean ComputeCountryScoreForOneLeague(int year, LeagueData league,
			Map<CountryData, Double> country_scores, Map<CountryData, Set<ClubData>> teams) {
		for (int i = year - kCountryScoreYears + 1; i <= year; ++i) {
			Collection<MatchData> matches = league.GetMatches(i);
			if (matches.IsEmpty()) return false;
			for (MatchData match : matches) {
				int home_goal = match.GetResult().home.stats.goals;
				int away_goal = match.GetResult().away.stats.goals;
				CountryData home_country = match.GetHomeClub().GetCountry();
				CountryData away_country = match.GetAwayClub().GetCountry();
				if (home_goal > away_goal) {
					country_scores.put(home_country, country_scores.get(home_country) + 3);
				} else if (home_goal < away_goal) {
					country_scores.put(away_country, country_scores.get(away_country) + 3);
				} else {
					country_scores.put(home_country, country_scores.get(home_country) + 1);
					country_scores.put(away_country, country_scores.get(away_country) + 1);
				}
				teams.get(home_country).add(match.GetHomeClub());
				teams.get(away_country).add(match.GetAwayClub());
			}
		}
		return true;
	}
	
	
	private static class ClubComparator implements Comparator<ClubData> {
		public ClubComparator(List<ClubData> ranked_league1_clubs, List<ClubData> ranked_league2_clubs) {
			this.ranked_league1_clubs = ranked_league1_clubs;
			this.ranked_league2_clubs = ranked_league2_clubs;
		}
		
		@Override
		public int compare(ClubData o1, ClubData o2) {
			int rank1 = GetRank(o1);
			int rank2 = GetRank(o2);
			if (rank1 < rank2) return -1;
			if (rank1 > rank2) return 1;
			return 0;
		}
		
		private int GetRank(ClubData club) {
			for (int i = 0; i < ranked_league1_clubs.size(); ++i) {
				if (club.equals(ranked_league1_clubs.get(i))) return i;
			}
			for (int i = 0; i < ranked_league2_clubs.size(); ++i) {
				if (club.equals(ranked_league2_clubs.get(i))) return i + ranked_league1_clubs.size();
			}
			return Integer.MAX_VALUE;
		}
		
		List<ClubData> ranked_league1_clubs;
		List<ClubData> ranked_league2_clubs;
	}
	
	private int[] country_ranks;
	private int[] country_rank_indices;
	
	private static int kCountryScoreYears = 5;
	
	public static int[][] kContinentalLeagueQuotas = {
			{ 4, 4, 4, 3, 3, 2, 2, 2, 2, 2, 2, 2 },
			{ 3, 3, 3, 3, 3, 3, 3, 3, 2, 2, 2, 2 },
	};
}
