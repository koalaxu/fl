package fl.engine.utils;

import java.util.Map;

import java.util.Map.Entry;
import java.util.TreeMap;

import fl.data.Ability;
import fl.data.Constant;
import fl.data.Constant.AbilityControlParameters;
import fl.data.Constant.AbilityControlParameters.AnnualChangeParameter;
import fl.data.Position.PositionType;
import fl.engine.data.PlayerData;

public class AbilityEstimator {
	public AbilityEstimator() {
		Constant constant = Constant.GetConstant();
		for (PositionType type : PositionType.values()) {
			Map<Integer, Map<Ability.Type, Double>> age_growth =
					new TreeMap<Integer, Map<Ability.Type, Double>>();
			estimated_growth.put(type, age_growth);
			for (int age = 17; age < kMaxAge; age++) {
				age_growth.put(age, new TreeMap<Ability.Type, Double>());
			}
		}
		for (Entry<Ability.Type, AbilityControlParameters> e : constant.ability_control_params.entrySet()) {
			for (int age = 17; age < kMaxAge; age++) {
				if (e.getValue().annual_change_params == null) continue;
				for (AnnualChangeParameter p : e.getValue().annual_change_params) {
					if (age <= p.age_threshold) {
						for (PositionType pos_type : PositionType.values()) {
							estimated_growth.get(pos_type).get(age).put(e.getKey(),
									p.inc_probability - p.dec_probability);
						}
						break;
					}
				}
			}
		}

		for (Entry<PositionType, Map<Ability.Type, Double>> entry :
			constant.position_ability_upgrade_map.entrySet()) {
			int exp = 0;
			for (int age = 17; age < kMaxAge; age++) {
				exp += AbilityController.GetExp(age, false) * kEstimatedMatchYear;
				int ability_boost = exp / AbilityController.kExpToPromote;
				if (age <= constant.free_annual_ability_age_threshold) {
					ability_boost++;
				}
				exp = exp % AbilityController.kExpToPromote;
				for (Entry<Ability.Type, Double> e : entry.getValue().entrySet()) {
					if (e.getKey() == null) continue;
					estimated_growth.get(entry.getKey()).get(age).put(
							e.getKey(), e.getValue() * ability_boost);
				}
			}
		}
	}
	
	public double EstimateFutureScore(PlayerData player, int years) {
		double base_score = PositionAnalyzer.GetOverallScore(player.GetInfo());
		return EstimateFutureScore(player, base_score, years);

	}
	
	public double EstimateFutureScore(PlayerData player, double base_score, int years) {
		double total_score = base_score;
		int max_years = Math.min(years, kMaxAge - player.GetAge() - 1);
		for (int i = 1; i <= max_years; ++i) {
			int age = player.GetAge() + i;
			double delta = PositionAnalyzer.GetOverallScore(
					player.GetInfo(),
					estimated_growth.get(player.GetInfo().GetPosition().type).get(age)::get);
			base_score += delta;
			if (age > Constant.GetConstant().retire_age_threshold) {
				base_score *= (1.0 - Constant.GetConstant().retire_probability);
			}
			total_score += base_score;
		}
		return total_score / (1 + years);
	}
						
	private Map<PositionType, Map<Integer, Map<Ability.Type, Double>>> estimated_growth =
			new TreeMap<PositionType, Map<Integer, Map<Ability.Type, Double>>>();
				
	private static int kEstimatedMatchYear = 20;
	private static int kMaxAge = 40;
}
