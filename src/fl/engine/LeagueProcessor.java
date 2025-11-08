package fl.engine;

import java.util.ArrayList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import fl.data.ClubInfo;
import fl.data.ClubRecord;
import fl.data.Constant;
import fl.data.League;
import fl.data.League.Type;
import fl.data.LeagueInfo;
import fl.data.Match;
import fl.data.MatchResult;
import fl.data.PlayerLeagueRecord;
import fl.data.Schedule;
import fl.data.Schedule.Round;
import fl.engine.IncomeCalculator.BroadcastingIncome;
import fl.engine.data.ClubData;
import fl.engine.data.CountryData;
import fl.engine.data.DataAccessor;
import fl.engine.data.LeagueData;
import fl.engine.data.LeagueRecordData;
import fl.engine.data.MatchData;
import fl.engine.data.PlayerData;
import fl.engine.utils.ContinentalLeagueUtil;
import fl.engine.utils.LeagueUtil;
import fl.engine.utils.LeagueUtil.FirstLegGoals;
import fl.engine.utils.StringLogger;
import fl.utils.FieldAccessor;
import fl.utils.StringUtil;

public class LeagueProcessor extends BaseComponent {

	protected LeagueProcessor(DataAccessor data_accessor, ComponentHub component_hub) {
		super(data_accessor, component_hub);
		LoadSchedule();
	}

	public void ProcessOneRound(Type league_type, Integer round_id) {
		long processed = 0;
		for (LeagueData league : GetAccessor().GetAllLeauges()) {
			if (league.GetData().type != league_type) continue;
			LeagueInfo info = league.GetInfo();
			if (info == null || info.team_ids == null) {
				System.out.println(league.GetData().name + " is unavailable this year.");
				continue;
			}
			ProcessOneLeague(league, league_schedule.get(league_type), FieldAccessor.kZeroBased.Get(round_id));
			DataAccessor.SetProgress((float)++processed / GetAccessor().GetAllLeagueSize());
		}
	}
	
	public void End() {
		league_incomes.clear();
		league_stats.clear();
		long processed = 0;
		for (LeagueData league : GetAccessor().GetAllLeauges()) {
			FinalizeOneLeague(league);
			DataAccessor.SetProgress((float)++processed / GetAccessor().GetAllLeagueSize() / 1.2f);
		}
		FinalizeClubFinance(league_incomes);
		DataAccessor.SetProgress(0.9f);
		RerankCountries();
		DataAccessor.SetProgress(0.92f);
		UpdateQualification();
		DataAccessor.SetProgress(0.95f);
		EstimateIncome();
		DataAccessor.SetProgress(1.0f);
	}
	
	private void ProcessOneLeague(LeagueData league, Schedule schedule, int round_id) {
		int year = GetAccessor().GetGlobal().year;
		Round round = schedule.rounds[round_id];
		LeagueInfo league_info = league.GetInfo();
		LeagueUtil league_util = new LeagueUtil(league);
		league_util.ComputeFanBase();
		System.out.println(league.GetData().name + " - Round " + round_id);
		boolean league_info_updated = false;
		List<MatchData> matches = new ArrayList<MatchData>();
		for (int match_id = 0; match_id < round.matches.length; ++match_id) {
			Schedule.Match scheduled_match = round.matches[match_id];
			Match match = new Match();
			match.home_club = league_info.team_ids.get(scheduled_match.host);
			match.away_club = league_info.team_ids.get(scheduled_match.away);
			match.year = year;
			match.league_id = league.GetKey();
			match.round_id = round_id;
			match.match_id = match_id;
			match.neutral_site = scheduled_match.neutral_site;
			match.knockout = scheduled_match.winner_id > 0;
			MatchData match_data = GetAccessor().CreateOneMatch(match);
			MatchResult first_leg_result = FindFirstLegResult(league, schedule, round_id, match_id);
			StringLogger logger = new StringLogger();
			GetComponentHub().match_simulator.ProceedOneMatch(match_data, first_leg_result, logger);
			FirstLegGoals first_leg_goals = LeagueUtil.GetFirstLegGoals(first_leg_result);
			long winner = GetWinnerClubId(match_data, first_leg_goals);
			league_info_updated |= UpdateMatchResult(
					match_data, league_info, scheduled_match, first_leg_goals, winner);
			match_data.Write();
			match_data.WriteCommentary(logger.GetString());
			league_util.UpdateFanChanges(GetAccessor().GetClub(match.home_club),
					GetAccessor().GetClub(match.away_club), winner, match_data);
			matches.add(match_data);
		}
		if (league_info_updated) league.WriteInfo();
		LeagueRecordData record = league.GetLeagueRecord(year);
		record.UpdateFromMatches(matches, round_id);
		record.Write(league_info);
	}
	
	private long GetWinnerClubId(MatchData match_data, FirstLegGoals first_leg_goals) {
		Match match = match_data.GetMatch();
		MatchResult result = match_data.GetResult();
		if (result.home.stats.abandoned) {
			return match.away_club;
		} else if (result.away.stats.abandoned) {
			return match.home_club;
		}
		int home_goals = result.home.stats.goals;
		int away_goals = result.away.stats.goals;
		if (first_leg_goals != null) {
			home_goals += first_leg_goals.away_goals;
			away_goals += first_leg_goals.home_goals;
		}
		home_goals += result.home.stats.shootout_goals;
		away_goals += result.away.stats.shootout_goals;
		if (home_goals > away_goals) {
			return match.home_club;
		} else if (home_goals < away_goals) {
			return match.away_club;
		}
		if (match.knockout) {
			System.err.println("Knockout shouldn't draw!");
			System.exit(0);
		}
		return 0L;
	}
	
	private boolean UpdateMatchResult(MatchData match_data, LeagueInfo league_info, Schedule.Match scheduled_match,
			FirstLegGoals first_leg_goals, long winner) {
		MatchResult result = match_data.GetResult();
		Match match = match_data.GetMatch();
		System.out.println(GetAccessor().GetClub(match.home_club).GetData().name + " " +
				result.home.stats.goals + " : " + result.away.stats.goals + " " + (first_leg_goals == null ?
						"" : "(" + first_leg_goals.away_goals + " : " + first_leg_goals.home_goals + ")") +
				" " + GetAccessor().GetClub(match.away_club).GetData().name +
				(result.extra_time ? ((result.home.stats.shootout_goals + result.away.stats.shootout_goals > 0) ?
					(" [PK " + result.home.stats.shootout_goals + ":" + result.away.stats.shootout_goals + "] ") :
						" [EX]") : ""));
		if (match.knockout) {
			if (league_info.team_ids.size() == scheduled_match.winner_id) {
				league_info.team_ids.add(winner);
			} else if (league_info.team_ids.get(scheduled_match.winner_id) == -1) {
				league_info.team_ids.set(scheduled_match.winner_id, winner);
			} else {
				System.err.println("winner_id and team_ids mismatch!!");
				System.exit(0);
			}
			return true;
		}
		return false;
	}
	
	private MatchResult FindFirstLegResult(LeagueData league, Schedule schedule, int round_id, int match_id) {
		Schedule.Match current_match = schedule.rounds[round_id].matches[match_id];
		if (current_match.winner_id <= 0) return null;
		if (round_id <= 0) return null;
		Schedule.Match first_leg_match = schedule.rounds[round_id - 1].matches[match_id];
		if (current_match.host == first_leg_match.away && current_match.away == first_leg_match.host) {
			MatchData first_leg = league.GetMatch(GetAccessor().GetGlobal().year, round_id - 1, match_id);
			return first_leg.GetResult();
		} else if (round_id == schedule.rounds.length - 1) {
			return null;  // Final (single match)
		}
		System.err.println("Unexpected Schedule: First Leg and Second Leg mismatch: " +
				league.GetData().name + " round: " + round_id + " match_id: " + match_id);
		System.exit(0);
		return null;
	}
	
	private void FinalizeOneLeague(LeagueData league) {
		System.out.println("Processing " + league.GetData().name + " ...");
		long year = GetAccessor().GetGlobal().year;
		// League
		LeagueRecordData league_record = league.GetLeagueRecord(year);
		if (!league_record.Started()) return;
		league_record.ComputeRank();
		BroadcastingIncome income = GetComponentHub().income_calculator.CalculateBroadcastingIncome(league);
		league_record.SetDomesticIncome(income.domestic_income);
		league_record.SetInternationalIncome(income.international_income);
		league_record.Write();
		league_incomes.put(league, income);
		league_stats.put(league, league_record);
		// Player
		Map<Long, PlayerLeagueRecord> player_stats = LeagueUtil.ComputePlayerStats(league.GetMatches(year));
		for (Entry<Long, PlayerLeagueRecord> e : player_stats.entrySet()) {
			PlayerData player = GetAccessor().GetPlayer(e.getKey());
			PlayerLeagueRecord record = e.getValue();
			player.GetLeagueInfo(league).AddLeagueRecord(record);
		}
	}
	
	private void FinalizeClubFinance(Map<LeagueData, BroadcastingIncome> league_incomes) {
		long year = GetAccessor().GetGlobal().year;
		IncomeCalculator cal = GetComponentHub().income_calculator;
		cal.Reset();
		long total_old_fund = 0L;
		long total_new_fund = 0L;
		for (ClubData club : GetAccessor().GetAllClubs()) {
			ClubInfo club_info = club.GetInfo();
			total_old_fund += club_info.fund;
			long income = 0L;
			for (LeagueData league : club.GetClubParticipatingLeagues()) {
				LeagueRecordData league_record = league_stats.get(league);
				if (league_record == null) continue;
				income += cal.DistributedIncome(
						league, club, league_incomes.get(league), league_record.GetClubRank(club),
						league_record.GetClubStats(club));
				for (MatchData match : league.GetMatches(year)) {
					if (!match.GetHomeClub().equals(club)) continue;
					income += cal.CalculateMatchdayIncome(club, cal.GetMatchDayAttendee(club, match));
				}
				
			}
			income += cal.CalculateCommericalIncome(club);
			
			long expense = 0L;
			for (PlayerData player : club.GetPlayers()) {
				expense += player.GetInfo().club_info.wage;
			}
			
			club_info.fund += income - expense;
			total_new_fund += club_info.fund;
			ClubRecord record = new ClubRecord();
			record.income = income;
			record.expense = expense;
			record.domestic_fans = club_info.domestic_fans;
			record.international_fans = club_info.international_fans;
			club.WriteInfo();
			club.AddRecord(record);
			System.out.println(club.GetData().name + "  + " + StringUtil.ShortNumber(income) + " - " +
					StringUtil.ShortNumber(expense) + " = " + StringUtil.ShortNumber(club_info.fund));
		}
		GetComponentHub().price_model.UpdatePriceBase(total_old_fund, total_new_fund);
	}
	
	private void RerankCountries() {
		LeagueData champion_league = null;
		LeagueData euro_league = null;
		for (LeagueData league : GetAccessor().GetAllLeauges()) {
			if (league.GetData().type != Type.CONTINENTAL_CUP) continue;
			if (league.GetData().level == 1) {
				champion_league = league;
			} else {
				euro_league = league;
			}
		}
		int[] new_rank = ContinentalLeagueUtil.ComputeCountryScore(GetAccessor().GetGlobal().year,
				GetAccessor().GetAllCountries(), champion_league, euro_league);
		if (new_rank != null) {
			System.err.println("new rank");
			for (int i : new_rank) System.err.println(i);
			GetAccessor().GetGlobal().country_ranks = new_rank;
		}
	}
	
	private void UpdateQualification() {
		ClubData champion_winner = null;
		ClubData euro_winner = null;
		LeagueData champion_league = null;
		LeagueData euro_league = null;
		LeagueData euro_super_league = null;
		int[] country_ranks = GetAccessor().GetGlobal().country_ranks;
		ContinentalLeagueUtil continental_league_util =	new ContinentalLeagueUtil(country_ranks);
		long[][] champion_league_qualified = new long[country_ranks.length][];
		long[][] euro_league_qualified = new long[country_ranks.length][];
		for (LeagueData league : GetAccessor().GetAllLeauges()) {
			if (league.GetData().type == Type.CONTINENTAL_SUPER_CUP) euro_super_league = league;
			if (league.GetData().type != Type.CONTINENTAL_CUP) continue;
			if (league.GetData().level == 1) {
				champion_league = league;
				champion_winner = league_stats.get(league).GetRankedTeams().get(0);
			} else {
				euro_league = league;
				euro_winner = league_stats.get(league).GetRankedTeams().get(0);
			}
		}
		UpdateSuperLeagueQualification(euro_super_league, champion_winner, euro_winner);
		List<ClubData> champion_qualified = new ArrayList<ClubData>();
		List<ClubData> euro_qualified = new ArrayList<ClubData>();
		for (CountryData country : GetAccessor().GetAllCountries()) {
			LeagueData league1 = country.GetLeague(Type.DOMESTIC_LEAGUE, 1);
			LeagueData league2 = country.GetLeague(Type.DOMESTIC_LEAGUE, 2);
			LeagueData cup = country.GetLeague(Type.DOMESTIC_CUP, 0);
			LeagueData super_cup = country.GetLeague(Type.DOMESTIC_SUPER_CUP, 0);
			List<ClubData> ranked_league1_clubs = league_stats.get(league1).GetRankedTeams();
			List<ClubData> ranked_league2_clubs = league_stats.get(league2).GetRankedTeams();
			List<ClubData> ranked_cup_clubs = league_stats.get(cup).GetRankedTeams();
			ClubData cup_winner = ranked_cup_clubs.get(0);
			// Super Cup
			UpdateSuperLeagueQualification(super_cup, ranked_league1_clubs.get(0),
					cup_winner.equals(ranked_league1_clubs.get(0)) ? ranked_cup_clubs.get(1) : cup_winner);

			// Continental Cups
			continental_league_util.FindContinentalLeagueQualifiedTeams(country, ranked_league1_clubs,
					champion_winner, euro_winner, cup_winner, champion_qualified, euro_qualified);
			champion_league_qualified[(int) (country.GetKey() - 1)] =
					continental_league_util.RerankQualifiedTeams(
							champion_qualified, ranked_league1_clubs, ranked_league2_clubs);
			euro_league_qualified[(int) (country.GetKey() - 1)] =
					continental_league_util.RerankQualifiedTeams(
							euro_qualified, ranked_league1_clubs, ranked_league2_clubs);
			
			// League/Cup
			for (int i = 0; i < ranked_league1_clubs.size() + ranked_league2_clubs.size(); ++i) {
				ClubData club = null;
				if (i < ranked_league1_clubs.size() - League.kPromotionQuota) {
					club = ranked_league1_clubs.get(i);
				} else if (i < ranked_league1_clubs.size()) {  // Promotion
					club = ranked_league2_clubs.get(i - ranked_league1_clubs.size() + League.kPromotionQuota);
				} else if (i < ranked_league1_clubs.size() + League.kPromotionQuota) {  // Relegation
					club = ranked_league1_clubs.get(i - League.kPromotionQuota);
				} else {
					club = ranked_league2_clubs.get(i - ranked_league1_clubs.size());
				}
				long club_id = club.GetKey();
				if (i < ranked_league1_clubs.size()) {
					league1.GetInfo().qualified_teams.set(i, club_id);
				} else {
					league2.GetInfo().qualified_teams.set(i - ranked_league1_clubs.size(), club_id);
				}
				cup.GetInfo().qualified_teams.set(i, club_id);
			}
			league1.WriteInfo();
			league2.WriteInfo();
			cup.WriteInfo();
		}
		continental_league_util.AssignChampionLeagueTeams(champion_league_qualified, champion_league.GetInfo());
		continental_league_util.AssignEuroLeagueTeams(euro_league_qualified, euro_league.GetInfo());
		champion_league.WriteInfo();
		euro_league.WriteInfo();
	}
	
	private void UpdateSuperLeagueQualification(LeagueData league, ClubData host, ClubData away) {
		league.GetInfo().qualified_teams = new ArrayList<Long>();
		league.GetInfo().qualified_teams.add(host.GetKey());
		league.GetInfo().qualified_teams.add(away.GetKey());
		league.WriteInfo();
	}
	
	private void EstimateIncome() {
		for (ClubData club : GetAccessor().GetAllClubs()) {
			long estimated_income = EstimateIncome(club);
			club.GetInfo().estimated_income = estimated_income;
			club.WriteInfo();
		}
	}
	
	private long EstimateIncome(ClubData club) {
		long income = 0L;
		IncomeCalculator cal = GetComponentHub().income_calculator;
		long matchday_income_per_match = cal.CalculateMatchdayIncome(club, 
				(long) Math.min(club.GetInfo().stadium_size,
						club.GetInfo().domestic_fans * club.GetCountry().GetData().fan_attend_ratio));
		int matches = 0;
		for (LeagueData league : club.GetClubParticipatingLeagues()) {
			LeagueRecordData record = league_stats.get(league);
			int rank = record != null ? record.GetClubRank(club) : 0;
			switch (league.GetData().type) {
				case DOMESTIC_LEAGUE:
					if (rank == 0) rank = league.GetInfo().qualified_teams.size() / 2;
					matches += league.GetInfo().qualified_teams.size() - 1;
					break;
				case DOMESTIC_CUP:
					matches += Math.log(league.GetInfo().qualified_teams.size() + 1 - rank) / Math.log(2) + 1;
					break;
				case CONTINENTAL_CUP:
					if (rank == 0) {
						rank = 25;
					} else if (rank > 16) {
						rank = 25;
					} else if (rank > 8) {
						rank = 21;
					} else {
						rank = 16;
					}
					matches += 3;
				default:
			}
			if (rank == 0) continue;
			income += cal.DistributedIncome(league, club, league_incomes.get(league), rank, null);
		}
		income += cal.CalculateCommericalIncome(club);
		income += matchday_income_per_match * matches;
		return income;
	}
	
	private static void LoadSchedule() {
		if (league_schedule != null) return;
		league_schedule = Constant.GetConstant().league_schedule;
	}
	
	private static Map<Type, Schedule> league_schedule;
	
	Map<LeagueData, BroadcastingIncome> league_incomes = new HashMap<LeagueData, BroadcastingIncome>();
	Map<LeagueData, LeagueRecordData> league_stats = new HashMap<LeagueData, LeagueRecordData>();
}
