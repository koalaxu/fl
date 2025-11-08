package fl.engine;

import java.util.ArrayList
;
import java.util.Comparator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import fl.data.ClubInfo;
import fl.data.Constant;
import fl.data.Country;
import fl.data.League;
import fl.data.League.Type;
import fl.engine.data.ClubData;
import fl.engine.data.CountryData;
import fl.engine.data.DataAccessor;
import fl.engine.data.LeagueData;
import fl.engine.data.LeagueRecordData;
import fl.engine.data.LeagueRecordData.LeagueTableData;
import fl.engine.data.MatchData;
import fl.engine.utils.ContinentalLeagueUtil;

public class IncomeCalculator extends BaseComponent {
	public IncomeCalculator(DataAccessor data_accessor, ComponentHub component_hub) {
		super(data_accessor, component_hub);
		Reset();
	}
	
	public class BroadcastingIncome {
		public long international_income = 0L;
		public long domestic_income = 0L;
		public SortedMap<Long, Long> per_country_income = new TreeMap<Long, Long>();
		public SortedMap<Long, List<Integer>> per_country_club = new TreeMap<Long, List<Integer>>();
		public long total_fans = 0L;
		public int total_wins = 0;
		public SortedMap<Integer, Integer> group_draws;
		public long GetTotalIncome() {
			return international_income + domestic_income;
		}
	}
	
	public void Reset() {
		continental_league_util = new ContinentalLeagueUtil(GetAccessor().GetGlobal().country_ranks);
	}

	public BroadcastingIncome CalculateBroadcastingIncome(LeagueData league_data) {
		Constant constant = Constant.GetConstant();
		League league = league_data.GetData();
		switch (league.type) {
		case DOMESTIC_LEAGUE:
			return CalculateLeagueIncome(
					league_data, constant.league_broadcasting_income[league.level - 1]);
		case DOMESTIC_CUP:
			return CalculateLeagueIncome(league_data, constant.cup_broadcasting_income);
		case DOMESTIC_SUPER_CUP:
			return CalculateLeagueIncome(league_data, constant.super_cup_broadcasting_income);
		case CONTINENTAL_CUP:
			return CalculateContinentalLeagueIncome(league_data,
					constant.continental_league_broadcasting_income[league.level - 1],
					constant.continental_league_broadcasting_income_unqualified_team[league.level - 1]);
		case CONTINENTAL_SUPER_CUP:
			return CalculateLeagueIncome(league_data, constant.continental_super_cup_broadcasting_income);
					
		}
		System.err.println("Income not implemented yet for " + league.type);
		return null;
	}
	
	public long CalculateCommericalIncome(ClubData club_data) {
		ClubInfo club_info = club_data.GetInfo();
		Country country = club_data.GetCountry().GetData();
		return (long) (((club_info.domestic_fans * country.fan_spend_ratio) + club_info.international_fans)
				* country.price_index);
	}
	
	public long CalculateMatchdayIncome(ClubData club, long attendee) {
		Country country = club.GetCountry().GetData();
		return (long) (attendee * Constant.GetConstant().ticket_price * country.price_index);
	}
	
	public int GetMatchDayAttendee(ClubData club, MatchData match) {
		int goal = match.GetResult().home.stats.goals;
		int conceded = match.GetResult().away.stats.goals;
		double multiplier = (goal > conceded ? 1.2 : (goal < conceded ? 0.7 : 0.9)) +
				Math.min(0.4, goal * 0.1) - Math.min(0.4, goal * 0.05);
		int max_attendee = (int) (club.GetInfo().domestic_fans * club.GetCountry().GetData().fan_attend_ratio
				* multiplier);
		return Math.min(club.GetInfo().stadium_size, max_attendee);
	}
	
	public long DistributedIncome(LeagueData league_data, ClubData club, BroadcastingIncome income,
			int rank, LeagueTableData club_stats) {
		long distributed_income = 0L;
		League league = league_data.GetData();
		int num_team = league_data.GetInfo().qualified_teams.size();
		long total_income = income.GetTotalIncome();
		switch (league.type) {
		case DOMESTIC_LEAGUE:
			distributed_income += income.international_income / num_team;
			distributed_income += income.domestic_income * 0.5 / num_team;
			distributed_income += income.domestic_income * 0.25 * GetDistributionShare(rank, num_team);
			// 25% will be distributed based on TV matches. Simulated by fan distribution.
			distributed_income += income.domestic_income * 0.25 *
					(club.GetInfo().domestic_fans + club.GetInfo().international_fans) / income.total_fans;
			break;
		case DOMESTIC_CUP:
			distributed_income += total_income * 0.2 / num_team;
			final double[] kRankShare = {0.287, 0.34, 0.188, 0.108, 0.067, 0.01 };  // Sum up to 1
			distributed_income += total_income * 0.8 * GetDistributionShare(rank, kRankShare);
			break;
		case CONTINENTAL_CUP:
			distributed_income += total_income * 0.25 / num_team;
			final double[] kCoontinentalCupRankShare = { 0.263, 0.145, 0.083, 0.052, 0.007 };  // Sum up to 0.55
			distributed_income += total_income * 0.30 *
					GetDistributionShare(rank, kCoontinentalCupRankShare);
			distributed_income += total_income * 0.30 * GetGroupDistributionShare(
					rank, league_data.GetGroupByTeam(club), club_stats, income.group_draws);  // Group share 0.45
			distributed_income += total_income * 0.3 * GetDistributionShare(rank, num_team);
			distributed_income += total_income * 0.15 * GetTvPoolDistributionShare(income, club, league.level);
			break;
		case DOMESTIC_SUPER_CUP:
		case CONTINENTAL_SUPER_CUP:
			distributed_income += total_income * (rank == 1 ? 0.5625 : 0.4375);
		default:
			break;
		}
//		System.err.println(league.type + " = " + distributed_income);
		return distributed_income;
	}
	
	private BroadcastingIncome CalculateLeagueIncome(LeagueData league, double income_multiplier) {
		CountryData country = league.GetCountry();
		if (country != null) {
			income_multiplier *= country.GetData().price_index;
		}
		List<ClubData> clubs = league.GetTeams();
		BroadcastingIncome income = new BroadcastingIncome();
		for (ClubData club : clubs) {
			income.domestic_income += club.GetInfo().domestic_fans * income_multiplier;
			income.international_income += club.GetInfo().international_fans * income_multiplier;
			income.total_fans += club.GetInfo().domestic_fans + club.GetInfo().international_fans;
		}
		
		return income;
	}
	
	private BroadcastingIncome CalculateContinentalLeagueIncome(
			LeagueData league, double income_multiplier, double unqualified_team_income_mutliplier) {
		BroadcastingIncome income = new BroadcastingIncome();
		int year = GetAccessor().GetGlobal().year;
		for (ClubData club : GetAccessor().GetAllClubs()) {
			long domestic_income = 0;
			CountryData country = club.GetCountry();
			long country_id = country.GetKey();
			if (league.HasClub(club)) {
				domestic_income = (long) (club.GetInfo().domestic_fans * income_multiplier);
				income.international_income += club.GetInfo().international_fans * income_multiplier;
				int rank = GetLastYearLeagueRank(club, country_id);
				List<Integer> club_ranks = income.per_country_club.get(country_id);
				if (club_ranks == null) {
					club_ranks = new ArrayList<Integer>();
					income.per_country_club.put(country_id, club_ranks);
				}
				club_ranks.add(rank > 0 ? rank : Integer.MAX_VALUE);
			} else {
				domestic_income = (long) (
						club.GetInfo().domestic_fans * unqualified_team_income_mutliplier);
			}		
			income.per_country_income.put(country_id,
					income.per_country_income.getOrDefault(country_id, 0L) + domestic_income);
			income.domestic_income += domestic_income;
		}
		for (List<Integer> club_ranks : income.per_country_club.values()) {
			club_ranks.sort(Comparator.naturalOrder());
		}
		LeagueRecordData record = league.GetLeagueRecord(year);
		if (record.Started()) {
			income.group_draws = new TreeMap<Integer, Integer>();
			for (int i = 0; i < League.kGroupNumber; ++i) {
				int draws = 0;
				for (int j = 0; j < League.kTeamPerGroup; ++j) {
					draws += record.GetGroupClubStats(i, j).stats.draw;
				}
				if (draws % 2 != 0) {
					System.err.println("Group draws should be even number. (Group " + i + ")");
					System.exit(0);
				}
				income.group_draws.put(i, draws / 2);
			}
		}
		return income;
	}
	
	private double GetGroupDistributionShare(
			int rank, int group_id, LeagueTableData club_stats, SortedMap<Integer, Integer> group_draws) {
		final double kPerGroupShare = 0.45 / League.kGroupNumber;
		final int kMatches = League.kTeamPerGroup * (League.kTeamPerGroup - 1) / 2;
		final double kPerGameShare = kPerGroupShare / kMatches;
		if (club_stats == null) {  // This is called to estimate
			final double[] kEstimatedShare = { 0.4, 0.3, 0.2, 0.1};
			return kPerGroupShare * kEstimatedShare[(rank - 1) / League.kGroupNumber];
		} else if (group_draws != null) {
			double draws = group_draws.get(group_id);
			if (draws == kMatches) return kPerGroupShare / League.kTeamPerGroup;
			double share = club_stats.stats.win * kPerGameShare;
			share += club_stats.stats.draw * kPerGameShare / 3;
			share += (club_stats.stats.win / (kMatches - draws)) * (draws * kPerGameShare / 3);
			return share;
		}
		// Shouldn't happen.
		System.err.println("Can not estimate group revenue share.");
		System.exit(0);
		return Double.NaN;
	}
	
	private double GetDistributionShare(int rank, int total_team) {
		return (total_team + 1.0 - rank) / ((total_team  + 1.0) * total_team / 2.0);
	}
	
	private double GetDistributionShare(int rank, double[] knockout_share) {
		double total_share = 0;
		for (int i = knockout_share.length - 1; i >= 0; --i) {
			if (rank > (1 << i)) break;
			total_share += knockout_share[i] / (1 << i);
		}
		return total_share;
	}
	
	private double GetTvPoolDistributionShare(BroadcastingIncome income, ClubData club, int level) {
		long country_id = club.GetCountry().GetKey();
		int clubs = income.per_country_club.get(country_id).size();
		int[][] per_country_quotas = ContinentalLeagueUtil.kContinentalLeagueQuotas;
		int country_rank = continental_league_util.GetCountryRank((int) country_id);
		int rank_threshold = per_country_quotas[0][country_rank] +
				(level == 2 ? per_country_quotas[1][country_rank] : 0);
		double share = 0.5 / clubs;
		int league_qualifed_teams = 0;
		int rank = GetLastYearLeagueRank(club, country_id);
		int relative_rank = -1;
		List<Integer> club_ranks = income.per_country_club.get(country_id);
		for (int i = 0; i < club_ranks.size(); ++i) {
			int club_rank = club_ranks.get(i);
			if (club_rank > rank_threshold) continue;
			league_qualifed_teams++;
			if (club_rank == rank) {
				relative_rank = i;
				break;
			}
		}
		if (league_qualifed_teams == 0) {
			share += 0.5 / clubs;
		} else {
			final double[][] kTvPoolShare = {
					{ 1 }, { 0.55, 0.45 }, { 0.45, 0.35, 0.2 }, { 0.4, 0.3, 0.2, 0.1 }
			};
			if (relative_rank >= 0) {
				share += 0.5 * kTvPoolShare[league_qualifed_teams - 1][relative_rank];
			}
		}
		return share;
	}
	
	private int GetLastYearLeagueRank(ClubData club, long country_id) {
		CountryData country = GetAccessor().GetCountry(country_id);
		LeagueData domestic_league = country.GetLeague(Type.DOMESTIC_LEAGUE, 1);
		LeagueRecordData record = domestic_league.GetLeagueRecord(GetAccessor().GetGlobal().year - 1);
		if (record == null || !record.Started()) return 0;
		return record.GetClubRank(club);
	}
	
	private ContinentalLeagueUtil continental_league_util;
}
