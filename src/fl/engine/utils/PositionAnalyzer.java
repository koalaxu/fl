package fl.engine.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import fl.data.Ability;
import fl.data.Ability.Type;
import fl.data.Constant;
import fl.data.Constant.PositionScoreParameters;
import fl.data.PlayerInfo;
import fl.data.Position;
import fl.data.Position.Location;
import fl.data.Position.PositionType;
import fl.utils.FieldAccessor;
import fl.utils.ScoredObject;

public class PositionAnalyzer {
	static public List<Position> GetReccommendPositions(PlayerInfo player_info) {
		List<Position> recommendation = new ArrayList<Position>();
		List<ScoredObject<Position>> positions = new ArrayList<ScoredObject<Position>>();
		for (PositionType pos_type : PositionType.values()) {
			if (pos_type == PositionType.GOAL_KEEPER) continue;
			double base_score = GetPositionConfidence(player_info, pos_type);
			for (Position pos : Position.GetPositionsFromType(pos_type)) {
				double score = base_score * kPositionCalibration.getOrDefault(pos, 1.0);
				if (pos.location == Position.Location.LEFT) {
					double left_foot_ability = FieldAccessor.kZeroBased.Get(player_info.left_foot);
					score *= (1.0 - (1.0 - left_foot_ability / PlayerInfo.kMaxFootAbility) * 0.1);
				} else if (pos.location == Position.Location.RIGHT) {
					double right_foot_ability = FieldAccessor.kZeroBased.Get(player_info.right_foot);
					score *= (1.0 - (1.0 - right_foot_ability / PlayerInfo.kMaxFootAbility) * 0.1);
				}
				positions.add(new ScoredObject<Position>(pos, score));
			}
		}
		positions.sort(ScoredObject.SortByScore());
		recommendation.add(positions.get(0).element);
		Position preferred_pos = recommendation.get(0);
		if (preferred_pos.location != Location.CENTER &&
				Math.abs(player_info.left_foot - player_info.right_foot) <= 1) {
			recommendation.add(preferred_pos.GetReversePosition());
		} else {
			recommendation.add(positions.get(1).element);
		}
		return recommendation;
	}
	
	static public double GetPositionConfidence(PlayerInfo player_info, PositionType pos_type) {
		return GetPositionConfidence(player_info, player_info.ability::GetAbility, pos_type);
	}
	
	static public double GetOverallScore(PlayerInfo player) {
		double score = GetOverallScore(player, player.GetPosition());
		if (player.secondary_position != null) {
			score = Math.max(score, GetOverallScore(player, player.GetSecondaryPosition()));
		}
		return score;
	}	
	
	static public double GetOverallScore(PlayerInfo player, Position position) {
		return GetOverallScore(player, player.ability::GetAbility, position);
	}
	
	static public double GetPositionConfidence(
			PlayerInfo player_info, Function<Type, Number> ability_getter, PositionType pos_type) {
		PositionScoreParameters params = Constant.GetConstant().position_score_params.get(pos_type);
		double conf = 0.0;
		for (Entry<Type, Double> e : params.ability_weights.entrySet()) {
			Number ability = ability_getter.apply(e.getKey());
			double d = (ability == null ? 0 : ability.doubleValue()) / Ability.kMaxAbility;
			conf += d * e.getValue();
		}
		if (player_info != null &&
				FieldAccessor.kZeroBased.Get(player_info.left_foot) +
				FieldAccessor.kZeroBased.Get(player_info.right_foot) > PlayerInfo.kMaxFootAbility + 1) {
			conf += params.inverse_foot_boost_score;
		}
		return conf;
	}
	
	static public double GetOverallScore(PlayerInfo player, Function<Type, Number> ability_getter) {
		double score = GetOverallScore(player, ability_getter, player.GetPosition());
		if (player.secondary_position != null) {
			score = Math.max(score, GetOverallScore(player, ability_getter, player.GetSecondaryPosition()));
		}
		return score;
	}	
	
	static public double GetOverallScore(
			PlayerInfo player, Function<Type, Number> ability_getter, Position position) {
		double conf = 0.9 * PositionAnalyzer.GetPositionConfidence(player, ability_getter, position.type);
		Ability.Type[] types = position.type == PositionType.GOAL_KEEPER ?
				Ability.kGkAbilties : Ability.kNoGkAbilties;
		for (Ability.Type type : types) {
			conf +=	0.1 * ability_getter.apply(type).doubleValue() / (Ability.kMaxAbility * types.length);
		}
		if (position.location == Position.Location.LEFT) {
			double left_foot_ability = FieldAccessor.kZeroBased.Get(player.left_foot);
			conf *= (1.0 - (1.0 - left_foot_ability / PlayerInfo.kMaxFootAbility) * 0.1);
		} else if (position.location == Position.Location.RIGHT) {
			double right_foot_ability = FieldAccessor.kZeroBased.Get(player.right_foot);
			conf *= (1.0 - (1.0 - right_foot_ability / PlayerInfo.kMaxFootAbility) * 0.1);
		}
		return conf;
	}
	
	public static double kUnfamiliarPositionDiscount = 0.8;
	
	
	private static Map<Position, Double> kPositionCalibration = new HashMap<Position, Double>() {
		private static final long serialVersionUID = 1L; {
		put(Position.Sweeper, 0.95); put(Position.CentreBack, 1.025);
		put(Position.LeftBack, 1.01); put(Position.RightBack, 0.965);
		put(Position.LeftMidFielder, 1.07); put(Position.RightMidFielder, 1.03);
		put(Position.DefensiveMidFielder, 0.98);
		put(Position.AttackingMidFielder, 1.02);
		put(Position.CentralMidFielder, 1.06);
		put(Position.CentreForward, 1.01); put(Position.SecondStriker, 0.975);
		put(Position.LeftWinger, 0.985); put(Position.RightWinger, 0.95);
	}};
}
