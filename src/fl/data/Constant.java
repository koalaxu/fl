package fl.data;

import java.util.Map;
import java.util.TreeMap;

import fl.data.League.Type;
import fl.data.Position.PositionType;
import fl.data.utils.JsonUtil;

public class Constant {
	public String base_dir;
	
	public class AbilityControlParameters {
		public double mean;
		public double std_dev;
		public class AnnualChangeParameter {
			public int age_threshold;
			public double inc_probability;
			public double dec_probability;
		}
		public AnnualChangeParameter[] annual_change_params;
	}
	
	public TreeMap<Ability.Type, AbilityControlParameters> ability_control_params;
	public double age_ability_variance;
	
	public class ExperienceParameter {
		public int age_threshold;
		public int exp_per_match;
	}
	public ExperienceParameter[] experience_parameters;
	public int free_annual_ability_age_threshold;
	public double second_division_experience_multiplier;
	public TreeMap<Integer, Integer> match_exp_thresholds;
	public TreeMap<Integer, Integer> gk_match_exp_thresholds;
	
	public Map<PositionType, Map<Ability.Type, Double>> position_ability_upgrade_map;
	
	public class PositionScoreParameters {
		public Map<Ability.Type, Double> ability_weights;
		public double inverse_foot_boost_score;
	}
	public Map<PositionType, PositionScoreParameters> position_score_params;
	
	public double left_foot_prob = 0.3;
	public double inverse_foot_odds = 0.2;
	
	public double[] league_broadcasting_income = {100.0, 15.0};
	public double cup_broadcasting_income = 20.0;
	public double super_cup_broadcasting_income = 3.0;
	public double continental_super_cup_broadcasting_income = 6.0;
	public double[] continental_league_broadcasting_income = {50.0, 20.0};
	public double[] continental_league_broadcasting_income_unqualified_team = {5.0, 0.0};
	public double ticket_price = 24;
	public double domestic_commercial_income = 100.0;
	public double international_commercial_income = 50.0;
	
	public int youth_promotion_per_year = 3;
	public int retire_age_threshold = 32;
	public double retire_probability = 0.4;
	
	public double max_wage_income_ratio = 0.8;
	public double max_wage_balance_ratio = 0.25;
	public double backup_wage_budget_multiplier = 0.25;
	
	public double training_injury_probability = 0.002;
	public int max_training_injury_length = 20;
	
	public GameParameter game_parameter = new GameParameter();
	
	public Map<Type, Schedule> league_schedule;
	
	static public Constant GetConstant() {
		if (constant == null) {
			constant = JsonUtil.ParseOneObjectFromJson("constants/constant.json", Constant.class);
			Map<Type, Schedule> league_schedule = new TreeMap<Type, Schedule>();
			league_schedule.put(Type.DOMESTIC_LEAGUE,
					JsonUtil.ParseOneObjectFromJson("constants/league_schedule.json", Schedule.class));
			league_schedule.put(Type.DOMESTIC_CUP,
					JsonUtil.ParseOneObjectFromJson("constants/cup_schedule.json", Schedule.class));
			league_schedule.put(Type.DOMESTIC_SUPER_CUP,
					JsonUtil.ParseOneObjectFromJson("constants/super_cup_schedule.json", Schedule.class));
			league_schedule.put(Type.CONTINENTAL_CUP,
					JsonUtil.ParseOneObjectFromJson("constants/champion_league_schedule.json", Schedule.class));
			league_schedule.put(Type.CONTINENTAL_SUPER_CUP,
					JsonUtil.ParseOneObjectFromJson("constants/super_cup_schedule.json", Schedule.class));
			constant.league_schedule = league_schedule;
		}
		return constant;
	}

	static public Constant constant;
}
