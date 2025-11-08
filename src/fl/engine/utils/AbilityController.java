package fl.engine.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import fl.data.Ability;
import fl.data.Constant;
import fl.data.PlayerInfo;
import fl.data.Position;
import fl.data.Position.PositionType;
import fl.data.Constant.AbilityControlParameters;
import fl.data.Constant.AbilityControlParameters.AnnualChangeParameter;
import fl.data.Constant.ExperienceParameter;
import fl.data.MatchStats;
import fl.engine.data.PlayerData;
import fl.utils.RandomUtil;
import fl.utils.WeightedSampler;

public class AbilityController {

	public static int GenerateInitialAbility(Ability.Type type, double bias) {
		AbilityControlParameters params = Constant.GetConstant().ability_control_params.get(type);
		return CapAbility(
				(int) Math.round(RandomUtil.SampleFromNormalDistribution(params.mean + bias, params.std_dev)));
	}
	
	public static int GetAnnualChange(Ability.Type type, int age) {
		AbilityControlParameters params = Constant.GetConstant().ability_control_params.get(type);
		for (AnnualChangeParameter p : params.annual_change_params) {
			if (age <= p.age_threshold) {
				if (RandomUtil.WhetherToHappend(p.inc_probability)) {
					return 1;
				} else if (RandomUtil.WhetherToHappend(p.dec_probability / (1.0 - p.inc_probability))) {
					return -1;
				}
				return 0;
			}
		}
		return 0;
	}
	
	public static int GetAgeVariance() {
		double var = Constant.GetConstant().age_ability_variance;
		if (RandomUtil.WhetherToHappend(var)) {
			return 1;
		} else if (RandomUtil.WhetherToHappend(var / (1.0 - var))) {
			return -1;
		}
		return 0;
	}
	
	public static void UpdateAnnualAbility(PlayerInfo player_info, int age) {
		for (Ability.Type type : Ability.kPhysicalAbilties) {
			Integer ability = player_info.ability.GetAbility(type);
			if (ability == null) continue;
			player_info.ability.SetAbility(type, CapAbility(ability + GetAnnualChange(type, age)));
		}
		if (age <= Constant.GetConstant().free_annual_ability_age_threshold) {
			BoostAbility(player_info);
		}
	}
	
	public static void BoostAbilityByExperience(PlayerInfo player_info) {
		if (player_info.exp == null) return;
		while (player_info.exp >= kExpToPromote) {
			player_info.exp -= kExpToPromote;
			BoostAbility(player_info);
		}
		if (player_info.exp == 0) player_info.exp = null;
	}
	
	 public static boolean GainExp(PlayerData player, MatchStats stats) {
		int score = ScoreStats(stats);
		TreeMap<Integer, Integer> thresholds = player.GetInfo().GetPosition() == Position.GoalKeeper ?
				Constant.GetConstant().gk_match_exp_thresholds : Constant.GetConstant().match_exp_thresholds;
		for (Entry<Integer, Integer> entry : thresholds.entrySet()) {
			if (player.GetAge() <= entry.getKey()) return score >= entry.getValue();
		}
		return false;
	}
 	
	public static int GetExp(int age, boolean is_second_division) {
		double multipler = is_second_division ?
			Constant.GetConstant().second_division_experience_multiplier : 1.0;
		for (ExperienceParameter param : Constant.GetConstant().experience_parameters) {
			if (age <= param.age_threshold) {
				return (int)(param.exp_per_match * multipler);
			}
		}
		return 0;
	}
	
	private static void BoostAbility(PlayerInfo player_info) {
		 Ability.Type type =
				 GetBoostSampler().get(Position.positions.get(player_info.position).type).Sample();
		 if (type == null) return;
		 player_info.ability.SetAbility(type, CapAbility(player_info.ability.GetAbility(type) + 1));
	}
	
	private static int CapAbility(int input) {
		return Math.max(1, Math.min(Ability.kMaxAbility, input));
	}
	
	private static HashMap<PositionType, WeightedSampler<Ability.Type>> GetBoostSampler() {
		if (ability_boost_sampler == null) {
			ability_boost_sampler = new HashMap<PositionType, WeightedSampler<Ability.Type>>();
			 for (Entry<PositionType, Map<Ability.Type, Double>> e :
				 Constant.GetConstant().position_ability_upgrade_map.entrySet()) {
				 WeightedSampler<Ability.Type> sampler = new WeightedSampler<Ability.Type>();
				 sampler.AddAll(e.getValue());
				 ability_boost_sampler.put(e.getKey(), sampler);
			 }
		}
		return ability_boost_sampler;
	}
	
	private static int ScoreStats(MatchStats stats) {
		int score = 0;
		score += stats.goal * 10;
		score += stats.assistance * 10;
		score += stats.dribble_succeed * 2;
		score += stats.shot;
		score += stats.shot_ontarget;
		score += stats.header_succeed;
		score += stats.key_pass * 5;
		score += stats.tackle * 2;
		score += stats.intecept * 2;
		score += stats.clearance;
		score += stats.save * 2;
		return score;
	}
	
	static private HashMap<PositionType, WeightedSampler<Ability.Type>> ability_boost_sampler;
	static public int kExpToPromote = 100;
}
