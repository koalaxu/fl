package fl.engine.utils;

import fl.data.Ability;

import fl.data.PlayerInfo;
import fl.data.Position;
import fl.data.Ability.Type;
import fl.engine.data.MatchInfo.PlayerState;

public class AbilityCalculator {
	public static void UpdateAbility(PlayerInfo player, Position pos, Ability ability) {
		for (Ability.Type type: pos == Position.GoalKeeper ? Ability.kGkAbilties : Ability.kNoGkAbilties) {
			double ability_multiplier = (player.GetPosition() == pos || player.GetSecondaryPosition() == pos) ?
					1.0 : PositionAnalyzer.kUnfamiliarPositionDiscount;
			if (player.condition != null) ability_multiplier = ability_multiplier * player.condition / 100;
			Integer base_ability = player.ability.GetAbility(type);
			if (base_ability == null) {
				base_ability = NonQualifiedAbility(player, type);
			}
			ability.SetAbility(type, (int) (base_ability * ability_multiplier));
		}
	}
	
	public static double ConditionMultiplier(Integer condition, Integer stamina) {
		double m = 1.0;
		if (condition != null) {
			m = m * condition / 100;
		}
		if (stamina != null) {
			m = m * StaminaMultipier(stamina);
		}
		return m;
	}
	
	public static double GetAbility(PlayerState player, Ability.Type type) {
		if (player.pos == Position.GoalKeeper && type == Type.POSITION) {
			type = Type.GOALKEEP;
		}
		Integer base_ability = player.ability.GetAbility(type);
		if (base_ability == null) {
			if ((player.pos == Position.GoalKeeper && type == Type.SHOT) ||
					(player.pos != Position.GoalKeeper && type == Type.GOALKEEP)) {
				base_ability = NonQualifiedAbility(player.player.GetInfo(), type);
			} else {
				System.err.println("Shouldn't happen");
				System.exit(0);
			}
		}
		return base_ability * AbilityCalculator.StaminaMultipier(player.stamina)
				* AbilityCalculator.DisabledMultiplier(player.disabled);
	}
	
	public static double StaminaMultipier(double stamina) {
		return 1.0 - kStaminaDecay0 * (1.0 - Math.max(0, Math.min(80, stamina) - 50) / 30)
				- kStaminaDecay1 * (1.0 - (Math.min(50, stamina) / 50));
	}
	
	public static double DisabledMultiplier(double disabled) {
		return Math.min(1.0, Math.max(0.0, 1 - disabled * kDisabledDecay));
	}
	
	public static double PlayerScore(PlayerInfo player, Position position) {
		return PlayerScore(player, player.stamina, position);
	}
	
	public static double PlayerScore(PlayerState player, Position position) {
		return PlayerScore(player.player.GetInfo(), (int) player.stamina, position);
	}
	
	private static double PlayerScore(PlayerInfo player, Integer stamina, Position position) {
		double score = PositionAnalyzer.GetPositionConfidence(player, position.type)
				* AbilityCalculator.ConditionMultiplier(player.condition, stamina);
		if (player.GetPosition() != position &&	player.GetSecondaryPosition() != position) {
			score *= PositionAnalyzer.kUnfamiliarPositionDiscount;
		}
		return score;
	}
	
	private static int NonQualifiedAbility(PlayerInfo player, Ability.Type type) {
		return (int) (((player.player_id * kRandomSalt1 + type.ordinal()) * kRandomSalt2) % 10);
	}
	
	private static double kStaminaDecay0 = 0.4;
	private static double kStaminaDecay1 = 0.2;
	private static double kDisabledDecay = 0.1;
	
	private static int kRandomSalt1 = 101;
	private static int kRandomSalt2 = 17;
}
