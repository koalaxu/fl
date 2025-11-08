package fl.engine.utils;

import java.util.ArrayList;
import java.util.List;

import fl.data.Ability;
import fl.data.Constant;
import fl.data.PlayerInfo;
import fl.data.Position;
import fl.data.Position.PositionName;
import fl.engine.data.PlayerData;
import fl.utils.RandomUtil;

public class YouthUtil {
	
	public static int GetNumberCandidates(int base_value, int youth_training_level, double multiplier) {
		return (int) Math.ceil(base_value * (1.0 + youth_training_level * multiplier));
	}
	
	public static List<PlayerInfo> GenerateCandidates(boolean is_gk, int num_candidates, int youth_training_level) {
		List<PlayerInfo> candidates = new ArrayList<PlayerInfo>();
		for (int i = 0; i < num_candidates; ++i) {
			candidates.add(InitilaizeOnePlayer(is_gk, youth_training_level));
		}
		return candidates;
	}
	
	public static PlayerInfo InitilaizeOnePlayer(boolean is_gk, int youth_training_level) {
		PlayerInfo player_info = new PlayerInfo();
		player_info.ability = new Ability();
		for (Ability.Type type : is_gk ? Ability.kGkAbilties : Ability.kNoGkAbilties) {
			player_info.ability.SetAbility(type,
					AbilityController.GenerateInitialAbility(type, youth_training_level * kBiasMultiplier));
		}
		if (!is_gk) {
			boolean is_left_foot = RandomUtil.WhetherToHappend(Constant.GetConstant().left_foot_prob);
			int inverse_foot_ability =
					RandomUtil.RepeatedBounolli(3, Constant.GetConstant().inverse_foot_odds);
			player_info.left_foot = is_left_foot? PlayerInfo.kMaxFootAbility : inverse_foot_ability;
			player_info.right_foot = is_left_foot? inverse_foot_ability : PlayerInfo.kMaxFootAbility ;
		}
		return player_info;
	}
	
	public static PlayerInfo InitilaizeOnePlayer(int youth_training_level) {
		boolean is_gk = RandomUtil.WhetherToHappend(1.0/11.0);
		PlayerInfo player_info = InitilaizeOnePlayer(is_gk, youth_training_level);
		if (is_gk) {
			player_info.position = PositionName.GOAL_KEEPER;
		} else {
			List<Position> recommended_pos = PositionAnalyzer.GetReccommendPositions(player_info);
			player_info.position = recommended_pos.get(0).name;
			if (recommended_pos.size() > 1) player_info.secondary_position = recommended_pos.get(1).name;
		}
		return player_info;
	}
	
	public static boolean WhetherToRetire(PlayerData player) {
		if (player.GetAge() < Constant.GetConstant().retire_age_threshold) return false;
		return RandomUtil.WhetherToHappend(Constant.GetConstant().retire_probability);
	}
	
	private static double kBiasMultiplier = 0.1;
}
