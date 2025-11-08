package fl.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import fl.data.Ability;
import fl.data.Club;
import fl.data.ClubInfo;
import fl.data.Country;
import fl.data.Formation;
import fl.data.Formation.FormationName;
import fl.data.Global;
import fl.data.InitializationSetup;
import fl.data.InitializationSetup.CountryParameters;
import fl.data.Position.PositionName;
import fl.data.Position.PositionType;
import fl.data.League;
import fl.data.LeagueInfo;
import fl.data.Player;
import fl.data.PlayerInfo;
import fl.data.Position;
import fl.data.utils.CsvReader;
import fl.data.utils.FileUtil;
import fl.data.utils.JsonUtil;
import fl.db.DataBase;
import fl.db.Key;
import fl.engine.IncomeCalculator;
import fl.engine.IncomeCalculator.BroadcastingIncome;
import fl.engine.data.ClubData;
import fl.engine.data.DataAccessor;
import fl.engine.data.LeagueData;
import fl.engine.data.PlayerData;
import fl.engine.utils.AbilityController;
import fl.engine.utils.ContinentalLeagueUtil;
import fl.engine.utils.FormationUtil;
import fl.engine.utils.NameGenerator;
import fl.engine.utils.PositionAnalyzer;
import fl.engine.utils.ProbabilisticNameGenerator;
import fl.engine.utils.SquadNumberAssigner;
import fl.engine.utils.WageModel;
import fl.engine.utils.YouthUtil;
import fl.utils.MathUtil;
import fl.utils.QuantileCalculator;
import fl.utils.RandomUtil;
import fl.utils.ScoredObject;
import fl.utils.WeightedSampler;

public class Initializer {
	public Initializer(InitializationSetup setup, String dir) {
		setup_ = setup;
		dir_ = dir;
		db_ = new DataBase(dir_);
		formation_sampler_.AddAll(setup.formation_popularity);
		club_names = CsvReader.ReadSingleColumnCsv("constants/club_names.txt");
	}
	
	public void Init() {
		FileUtil.DeleteOneDir(dir_);
		db_.Load();
		
		global.year = setup_.start_year;
		global.week = 0;
		global.mid_week = false;
		ContinentalLeagueUtil continental_league_util = new ContinentalLeagueUtil(global.country_ranks);
		JsonUtil.WriteOneObjectToJson(dir_ + "global.json", global);
		
		long[][] champions_league_teams = new long[setup_.countries.length][];
		long[][] euro_league_teams = new long[setup_.countries.length][];
		for (int i = 0; i < setup_.countries.length; ++i) {
			InitDomesticData(i, champions_league_teams, euro_league_teams);
		}
		League league = new League();
		league.type = League.Type.CONTINENTAL_CUP;
		league.name = "Champions League";
		league.level = 1;
		Key league_key = db_.league_table.AppendRow(league);
		LeagueInfo league_info = new LeagueInfo();
		league_info.league_id = league_key.keys[0];
		continental_league_util.AssignChampionLeagueTeams(champions_league_teams, league_info);
		db_.league_info_table.UpdateRow(league_info);
		league = new League();
		league.type = League.Type.CONTINENTAL_CUP;
		league.name = "Euro Leauge";
		league.level = 2;
		league_key = db_.league_table.AppendRow(league);
		league_info = new LeagueInfo();
		league_info.league_id = league_key.keys[0];
		continental_league_util.AssignEuroLeagueTeams(euro_league_teams, league_info);
		db_.league_info_table.UpdateRow(league_info);
		league = new League();
		league.type = League.Type.CONTINENTAL_SUPER_CUP;
		league.name = "Euro Super Cup";
		db_.league_table.AppendRow(league);
		
		db_.Flush();
		InitWages();
		db_.Flush();
	}
	
	private void InitDomesticData(
			int index, long[][] champions_league_teams, long[][] euro_league_teams) {
		Country country = new Country();
		champions_league_teams[index] = new long[ContinentalLeagueUtil.kContinentalLeagueQuotas[0][index]];
		euro_league_teams[index] = new long[ContinentalLeagueUtil.kContinentalLeagueQuotas[1][index]];
		CountryParameters country_param = setup_.countries[index];
		String country_name = country_param.name;
		System.out.println("Initializing " + country_name + " ...");
		country.name = country_name;
		country.fan_attend_ratio = country_param.fan_attend_ratio;
		country.fan_spend_ratio = country_param.fan_spend_ratio;
		country.fan_population = country_param.fan_population;
		country.price_index = country_param.price_index;
		long global_fan_share = (long) (setup_.global_fan_base * RandomUtil.PowerLaw(
				setup_.rank_x_lowerbound + index, setup_.fan_base_powerlaw_k));
		Key country_key = db_.country_table.AppendRow(country);
		LeagueInfo cup_info = new LeagueInfo();
		cup_info.qualified_teams = new ArrayList<Long>();
		for (int i = 0; i < setup_.division_per_country; ++i) {
			League league = new League();
			league.country_id = country_key.keys[0];
			league.level = i + 1;
			league.type = League.Type.DOMESTIC_LEAGUE;
			league.name = country_name + " Division " + league.level;
			Key league_key = db_.league_table.AppendRow(league);
			LeagueInfo league_info = new LeagueInfo();
			league_info.league_id = league_key.keys[0];
			league_info.qualified_teams = new ArrayList<Long>();
			List<ScoredObject<Double>> club_scored_ranks = new ArrayList<ScoredObject<Double>>();
			for (int j = 0; j < setup_.club_per_division; ++j) {
				double rank = RandomUtil.SampleFromUniformDistribution(
						 setup_.club_per_division * i,
						(double)setup_.club_per_division * (i + 1) - 1);
				club_scored_ranks.add(new ScoredObject<Double>(rank,
						rank + RandomUtil.SampleFromNormalDistribution(0, 1)));  
			}
			// Sort by "rank with noise" to determine the qualification of continental leagues.
			// The raw "rank" will determine the power of the club.
			club_scored_ranks.sort(ScoredObject.SortByScoreAscending());
			for (int j = 0; j < setup_.club_per_division; ++j) {
				Club club = new Club();
				club.country_id = country_key.keys[0];
				// club.name = country_name + " Club " + (i * setup_.club_per_division + j);
				club.name = club_names.get(index * setup_.division_per_country * setup_.club_per_division +
						i * setup_.club_per_division + j);
				Key club_key = db_.club_table.AppendRow(club);
				double rank = club_scored_ranks.get(j).element;
				club_ranks.put(club_key.keys[0], (int)(1 + Math.round(rank)));
				ClubInfo club_info = InitClub(country, global_fan_share, i, rank, country_param);
				club_info.club_id = club_key.keys[0];
				db_.club_info_table.UpdateRow(club_info);
				league_info.qualified_teams.add(club_key.keys[0]);
				cup_info.qualified_teams.add(club_key.keys[0]);
				if (i == 0 && j < champions_league_teams[index].length) {
					champions_league_teams[index][j] = club_key.keys[0];
				} else if (i == 0 &&
						j < champions_league_teams[index].length + euro_league_teams[index].length) {
					euro_league_teams[index][j - champions_league_teams[index].length] = club_key.keys[0];
				}
				int club_per_country = setup_.club_per_division * setup_.division_per_country;
				double relative_rank = 
						(club_per_country - 0.5 - rank) / club_per_country;
				List<PlayerInfo> players =
						InitPlayerData(country, club_info, relative_rank, country_param.mean_games, i == 1);
				ChooseFormation(club_info, players);
			}
			db_.league_info_table.UpdateRow(league_info);
		}
		League cup = new League();
		cup.country_id = country_key.keys[0];
		cup.type = League.Type.DOMESTIC_CUP;
		cup.name = country_name + " Cup";
		Key cup_key = db_.league_table.AppendRow(cup);
		cup_info.league_id = cup_key.keys[0];
		db_.league_info_table.UpdateRow(cup_info);
		cup = new League();
		cup.country_id = country_key.keys[0];
		cup.type = League.Type.DOMESTIC_SUPER_CUP;
		cup.name = country_name + " Super Cup";
		db_.league_table.AppendRow(cup);		
	}
	
	private ClubInfo InitClub(
			Country country, long global_fan_share, int division, double rank,
			CountryParameters country_param) {
		ClubInfo club_info = new ClubInfo();
		club_info.SetYouthTrainingLevel((int)Math.round((country_param.mean_youth_level / (1 + division)
				+ RandomUtil.SampleFromNormalDistribution(0, 2))));
		club_info.favorite_formation = formation_sampler_.Sample();
		int club_per_country = setup_.club_per_division * setup_.division_per_country;
		club_info.stadium_size =
				(int) ((club_per_country - rank) / club_per_country
				* (setup_.max_stadium_size - setup_.min_stadium_size) + setup_.min_stadium_size);
		club_info.domestic_fans = (long) (country_param.fan_base * RandomUtil.PowerLaw(
				setup_.domestic_rank_x_lowerbound + rank, setup_.fan_base_powerlaw_k)) * 1000;
		club_info.international_fans = (long) (global_fan_share * RandomUtil.PowerLaw(
				1 + rank, setup_.fan_base_powerlaw_k)) * 1000;
		club_info.fund = (long)
				(Math.min(club_info.domestic_fans * country_param.fan_attend_ratio,
						club_info.stadium_size) * country.price_index * 500);
		return club_info;
	}
	
	private List<PlayerInfo> InitPlayerData(
			Country country, ClubInfo club_info, double relative_club_rank, int mean_games,
			boolean is_second_division) {
		List<PlayerInfo> players = new ArrayList<PlayerInfo>();
		int adjusted_youth_training_level = Math.max(0,
				(int)  RandomUtil.InverseNormalDistributionCDF(
						club_info.youth_training_level.doubleValue(),
						setup_.std_dev_youth_level_adjustment, relative_club_rank));
		// Init GK
		int num_candidate = YouthUtil.GetNumberCandidates(
				setup_.goalkeeper_per_club, adjusted_youth_training_level, 1);
		List<PlayerInfo> candidates = YouthUtil.GenerateCandidates(
				true, num_candidate, adjusted_youth_training_level);
		List<ScoredObject<PlayerInfo>> scored_candidates = new ArrayList<ScoredObject<PlayerInfo>>();
		for (PlayerInfo candidate : candidates) {
			scored_candidates.add(new ScoredObject<PlayerInfo>(
					candidate,PositionAnalyzer.GetPositionConfidence(candidate, PositionType.GOAL_KEEPER)));
		}
		scored_candidates.sort(ScoredObject.SortByScore());
		for (int i = 0; i < setup_.goalkeeper_per_club; ++i) {
			PlayerInfo candidate = scored_candidates.get(i).element;
			candidate.position = PositionName.GOAL_KEEPER;
			players.add(candidate);
		}
		// Init non-GK
		num_candidate = YouthUtil.GetNumberCandidates(
				setup_.player_per_club - setup_.goalkeeper_per_club, adjusted_youth_training_level, 2);
		candidates = YouthUtil.GenerateCandidates(false, num_candidate, adjusted_youth_training_level);
		scored_candidates.clear();
		for (PlayerInfo candidate : candidates) {
			Ability ability = candidate.ability;
			double score = (ability.pace + ability.strength + ability.jump + ability.agility) / 2.0 +
					+ (ability.tackle + ability.position) / 2.0
					+ (ability.dribble + ability.pass + ability.shot) / 3.0;
			scored_candidates.add(new ScoredObject<PlayerInfo>(candidate, score));
		}
		scored_candidates.sort(ScoredObject.SortByScore());
		for (int i = 0; i < setup_.player_per_club - setup_.goalkeeper_per_club; ++i) {
			PlayerInfo candidate = scored_candidates.get(i).element;
			List<Position> recommended_pos = PositionAnalyzer.GetReccommendPositions(candidate);
			candidate.position = recommended_pos.get(0).name;
			if (recommended_pos.size() > 1) candidate.secondary_position = recommended_pos.get(1).name;
			players.add(candidate);
		}
		for (PlayerInfo player_info : players) {
			Player player = new Player();
			player.name = name_generator_.GenerateOneName(country);
			player.birth_year = global.year  - RandomUtil.SampleFromUniformDistribution(18, 32);
			// Simulate history of players
			int match_per_season = (int) RandomUtil.InverseNormalDistributionCDF(
					mean_games, 15, relative_club_rank);
			match_per_season = Math.min(60, Math.max(0,
					(int) RandomUtil.SampleFromNormalDistribution(match_per_season, 10)));
			int exp = 0;
			for (int age = 17; age < global.year - player.birth_year; age++) {
				AbilityController.UpdateAnnualAbility(player_info, age);
				match_per_season = Math.min(65, Math.max(0,
						(int) RandomUtil.SampleFromNormalDistribution(match_per_season, 5)));
				exp += match_per_season * AbilityController.GetExp(age, is_second_division);
			}
			player_info.exp = exp;
			AbilityController.BoostAbilityByExperience(player_info);
			player_info.exp = 0;
			player_info.club_id = club_info.club_id;
			player_info.club_info = player_info.new PlayerClubInfo();
			player_info.club_info.contract =
					Math.min(RandomUtil.SampleFromUniformDistribution(3, 5),
							34 + player.birth_year - global.year);
			Key key = db_.player_table.AppendRow(player);
			player_info.player_id = key.keys[0];		
		}
		SquadNumberAssigner.AssignSquadNumber(players);
		for (PlayerInfo player_info : players) {
			db_.player_info_table.UpdateRow(player_info);
		}
		return players;
	}
	
	private void ChooseFormation(ClubInfo club, List<PlayerInfo> players) {
		Formation favorite = null;
		int min_mismatch = 23;
		for (Formation formation : Formation.formations.values()) {
			List<Position> positions = FormationUtil.GetFormationPositions(formation);
			Set<PlayerInfo> selected_players = new HashSet<PlayerInfo>();
			for (int i = 0; i < 2; ++i) {
				for (Position pos : positions) {
					PlayerInfo selected_player = null;
					for (int j = 0; j < 2; ++j) {
						for (PlayerInfo player : players) {
							if (pos == (j == 0 ? player.GetPosition() : player.GetSecondaryPosition())
									&& !selected_players.contains(player)) {
								selected_player = player;
								break;
							}
						}
						if (selected_player != null) break;
					}
					if (selected_player != null) {
						selected_players.add(selected_player);
					}
				}
			}
			int mismatch = 22 - selected_players.size();
			if (mismatch < min_mismatch) {
				favorite = formation;
				min_mismatch = mismatch;
			}
		}
		club.favorite_formation = favorite.name;
	}
	
	private void InitWages() {
		DataAccessor accessor = DataAccessor.CreateDataAccessor(dir_);
		IncomeCalculator cal = new IncomeCalculator(accessor, null);
		HashMap<Long, BroadcastingIncome> league_income = new HashMap<Long, BroadcastingIncome>();
		List<LeagueInfo> leagues = new ArrayList<LeagueInfo>();
		for (LeagueData league : accessor.GetAllLeauges()) {
			league_income.put(league.GetKey(), cal.CalculateBroadcastingIncome(league));
			if (cal.CalculateBroadcastingIncome(league) != null)
			leagues.add(db_.league_info_table.FindRow(new Key(league.GetKey())));
		}
		List<ScoredObject<ClubData>> club_incomes = new ArrayList<ScoredObject<ClubData>>();
		Map<ClubData, Double> club_income_rank = new HashMap<ClubData, Double>();
		for (ClubData club : accessor.GetAllClubs()) {
			long income = 0L;
			long matchday_income_per_match = cal.CalculateMatchdayIncome(club, 
					(long) Math.min(club.GetInfo().stadium_size,
							club.GetInfo().domestic_fans * club.GetCountry().GetData().fan_attend_ratio));
			int matches = 0;
			for (LeagueData league : club.GetClubParticipatingLeagues()) {
				int rank = 0;
				switch (league.GetData().type) {
					case DOMESTIC_LEAGUE:
						rank = club_ranks.get(club.GetKey()) -
							setup_.club_per_division * (league.GetData().level - 1);
						matches += setup_.club_per_division - 1;
						break;
					case DOMESTIC_CUP:
						rank = club_ranks.get(club.GetKey());
						matches += 1;
						break;
					case CONTINENTAL_CUP:
						matches += 3;
						rank = 25;
					default:
				}
				if (rank == 0) continue;
				income += cal.DistributedIncome(league, club, league_income.get(league.GetKey()), rank, null);
			}
			income += cal.CalculateCommericalIncome(club);
			income += matchday_income_per_match * matches;
			club.GetInfo().estimated_income = income;
			club_incomes.add(new ScoredObject<ClubData>(club, income));
			db_.club_info_table.UpdateRow(club.GetInfo());
		}
		club_incomes.sort(ScoredObject.SortByScoreAscending());
		for (int i = 0; i < club_incomes.size(); ++i) {
			club_income_rank.put(club_incomes.get(i).element,
					Math.min(0.975, Math.max(0.025, (double)i / club_incomes.size())));
		}
		WageModel wage_model = new WageModel();
		for (ClubData club : accessor.GetAllClubs()) {
			for (PlayerData player : club.GetPlayers()) {
				PlayerInfo player_info = player.GetInfo();
				player_info.club_info.wage = Math.max(WageModel.kMinWage, (int) MathUtil.RoudByThousand(
						(long)wage_model.Predicte(player_info, club_income_rank.get(club))));
				db_.player_info_table.UpdateRow(player_info);
			}
		}
	}
	
	public static void main(String[] args) {
		FileUtil.Init();
		String dir = "test/";
		if (args.length > 0) {
			dir = args[0] + "/";
		}
		String initialization_file = "constants/initialization_setup_2021.json";
		if (args.length > 1) {
			initialization_file = args[1];
		}
		InitializationSetup setup =
				JsonUtil.ParseOneObjectFromJson(initialization_file, InitializationSetup.class);
		Initializer initializer = new Initializer(setup, dir);
		initializer.Init();
		
		DataAccessor accessor = DataAccessor.CreateDataAccessor(dir);
		TreeMap<PositionName, List<Double>> counts = new TreeMap<PositionName, List<Double>>();
		List<Double> club_stats = new ArrayList<Double>();
		for (PositionName pos : Position.positions.keySet()) {
			counts.put(pos, new ArrayList<Double>());
		}
		for (ClubData club : accessor.GetAllClubs()) {
			TreeSet<Double> top = new TreeSet<Double>();
			for (PlayerData player_data : club.GetPlayers()) {

			PlayerInfo player = player_data.GetInfo();
			Position position = player.GetPosition();
			double conf = PositionAnalyzer.GetPositionConfidence(player, position.type);
			counts.get(position.name).add(conf * 100);
			top.add(PositionAnalyzer.GetOverallScore(player) * 100);
			}
			double total_conf = 0;
			for (int i = 0; i < 22; ++i) {
				if (i < 11) {
					total_conf += top.pollLast();
				} else total_conf += top.pollLast() * 0.5;
			}
			club_stats.add(total_conf / 16.5);
		}
		double[] desired_quantiles = {0, 0.01, 0.05, 0.25, 0.5, 0.75, 0.95, 0.99, 1};
		for (PositionName pos : Position.positions.keySet()) {
			System.err.print(pos + " : "  + " (");
			if (!counts.get(pos).isEmpty()) {
				double[] ret = QuantileCalculator.Compute(desired_quantiles, counts.get(pos));
				for (double d : ret) {
					System.err.print((int)d + " ");
				}
			}
			System.err.print(")");
			System.err.println(" - " + (double)counts.get(pos).size() / 432);
		}
		
		
		double[] ret = QuantileCalculator.Compute(desired_quantiles, club_stats);
		System.err.println(club_stats.size());
		System.err.print("club : "  + " (");
		for (double d : ret) {
			System.err.print((int)d + " ");
		}
		System.err.println(")");
	}
	
	private InitializationSetup setup_;
	private String dir_;
	
	private DataBase db_;
	private Global global = new Global();
	private NameGenerator name_generator_ =
		new ProbabilisticNameGenerator("constants/first_initial.csv", "constants/last_name.csv");
	private WeightedSampler<FormationName> formation_sampler_ = new WeightedSampler<FormationName>();
	private TreeMap<Long, Integer> club_ranks = new TreeMap<Long, Integer>();
	private List<String> club_names;
	
}
