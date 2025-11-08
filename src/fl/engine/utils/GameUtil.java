package fl.engine.utils;

import java.util.Collection;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;

import fl.data.Ability;
import fl.data.Position;
import fl.data.Ability.Type;
import fl.engine.data.Game;
import fl.engine.data.Game.ActionType;
import fl.engine.data.MatchInfo.PlayerState;
import fl.engine.data.MatchInfo.TeamInfo;
import fl.utils.GeometryUtil;
import fl.utils.Point;
import fl.utils.RandomUtil;
import fl.utils.Vector;

public class GameUtil {	
	public static void UpdateStamina(PlayerState player, ActionType action_type, int times) {
		player.stamina = Math.max(0, player.stamina - kActionEnergyUsages.getOrDefault(action_type, 0.0) * times);
	}
	
	public static void UpdateDisabled(PlayerState player, ActionType action_type, boolean success) {
		player.disabled = (success ? kActionDisabledSuccuess : kActionDisabledFailure).getOrDefault(action_type, 0);
	}
	
	public static int UpdateDisabled(PlayerState player, int time_past) {
		int remain = time_past - player.disabled;
		if (remain >= 0) {
			player.disabled = 0;
			return remain;
		}
		player.disabled= -remain;
		return 0;
	}
	
	public static int PassTime(double length) {
		return (int) (Math.round(length / 50) + 2);
	}
	
	public static int ShotTime(double length) {
		return (int) (Math.round(length / 100) + 2);
	}
	
	public static boolean PassHitTarget(PlayerState player, double distance) {
		double ability = player.GetAbility(Ability.Type.PASS);
		if (distance < Game.kShortPassDistance) return true;
		if (distance < Game.kMediumPassDistance) return RandomUtil.WhetherToHappend((ability + 80.0) / 100.0);
		if (distance < Game.kLongPassDistance) return RandomUtil.WhetherToHappend((ability * 1.5 + 65.0) / 100.0);
		return RandomUtil.WhetherToHappend((ability * 1.5 + 60.0) / 100.0);
	}
	
	public static boolean GoalSave(double delta_ability) {
		return RandomUtil.WhetherToHappend(Math.max(-0.15, Math.min(0.3, delta_ability / 12)) + 0.45);
	}
	
	public static boolean Win(double delta_ability) {
		return RandomUtil.WhetherToHappend(Math.max(-0.4, Math.min(0.4, delta_ability / 8)) + 0.5);
	}
	
	public static boolean Win(double ability0, double ability1) {
		return Win(ability0 - ability1);
	}
	
	public static double GetSpeed(PlayerState player) {
		//return 29 / 4;
		return (player.GetAbility(Ability.Type.PACE) * 0.6 + 23) / 4;
	}
	
	public static void BallOffTarget(Point target, double min_offset, double max_offset, double pass_distance,
			Point ball) {
		max_offset = min_offset + (max_offset - min_offset) * Math.min(1.0, Math.max(0.0,
				(pass_distance - Game.kShortPassDistance) / (Game.kLongPassDistance - Game.kShortPassDistance)));
		max_offset = 100; // For test
		PitchUtil.RandomPointinRadius(target, min_offset, max_offset, ball);
	}
	
	public static void BallMoved(Point base, Point from, double min_offset, double max_offset, Point ball) {
		Vector direction = new Vector(from, base);
		if (direction.Length() < PitchUtil.kEpislon) {
			PitchUtil.RandomPointinRadius(base, min_offset, max_offset, ball);
			return;
		}
		BallMoved(base, direction, min_offset, max_offset, Math.PI / 6, ball);
	}
	
	public static void BallMoved(
			Point base, Vector direction, double min_offset, double max_offset, double angle_variance, Point ball) {
		Vector offset = new Vector(direction);
		offset.Scale(RandomUtil.SampleFromUniformDistribution(min_offset, max_offset));
		GeometryUtil.Rotate(offset, RandomUtil.SampleFromUniformDistribution(-angle_variance, angle_variance));
		ball.Copy(base);
		ball.Add(offset);
	}
	
	public static boolean IsGoalKeeper(PlayerState player) {
		return player.pos == Position.GoalKeeper;
	}
	
	public static double GetChallengeAbilityDelta(
			PlayerState attacker, PlayerState defender, ActionType type, double offset, boolean competition) {
		double attacker_ability = 0;
		double defender_ability = 0;
		switch (type) {
		case HEADER:
			attacker_ability = attacker.GetAbility(Type.JUMP);
			defender_ability = defender.GetAbility(Type.JUMP);
			break;
		case DRIBBLE:
			attacker_ability = attacker.GetAbility(RandomUtil.WhetherToHappend(0.5)?
					(competition ? Type.STRENGTH : Type.PACE) : Type.DRIBBLE);
			defender_ability = defender.GetAbility(RandomUtil.WhetherToHappend(0.5)?
					(competition ? Type.STRENGTH : Type.AGILITY) : Type.TACKLE);
			break;
		case PASS:
			attacker_ability = (attacker.GetAbility(Type.AGILITY) + attacker.GetAbility(Type.PASS)) / 2;
			defender_ability = (defender.GetAbility(Type.AGILITY) + defender.GetAbility(Type.POSITION)) / 2;
			break;
		case SHOT:
			attacker_ability = (attacker.GetAbility(Type.AGILITY) + attacker.GetAbility(Type.SHOT)) / 2;
			defender_ability = GetShotBlockAbility(defender);
			break;
		default:
			System.err.println("Unsupported challenging action: " + type);
			System.exit(0);
		}
		if (defender.pos == Position.GoalKeeper) offset += 2;
		return defender_ability - attacker_ability + offset;
	}
	
	public static double GetPassCutAbility(PlayerState player, boolean volley) {
		return (player.GetAbility(Type.POSITION) +
				player.GetAbility(volley ? Type.JUMP : Type.AGILITY)) / 2;
	}
	
	public static double GetGKAbility(PlayerState player, double distance, boolean volley) {
		double agility_weight = 0.4 * Math.max(0.0, (80.0 - distance) / 80.0);
		double jump_weight = RandomUtil.WhetherToHappend(volley ? 0.5 : 0.3) ? 0.2 : 0.0;
		return (agility_weight * player.GetAbility(Type.AGILITY) +
				jump_weight * player.GetAbility(Type.JUMP) +
				(1.0 - agility_weight - jump_weight) * player.GetAbility(Type.GOALKEEP) 
				-0.0 - (player.disabled * 2.0)) * 1.5;
 	}
	
	public static double GetShotAbility(
			PlayerState player, double distance_to_goal, boolean volley, boolean interfered) {
		return (volley ? player.GetAbility(Type.JUMP) :
				Math.max(0, player.GetAbility(Type.SHOT) - Math.max(0,
				(distance_to_goal - player.GetAbility(Type.STRENGTH) * 5 + 5) / 20))) * (interfered ? 0.8 : 1.0);
	}
	
	public static double GetShotBlockAbility(PlayerState player) {
		return (player.GetAbility(Type.AGILITY) * 2 +
				player.GetAbility(Type.POSITION) + player.GetAbility(Type.TACKLE)) / 4;
	}
	
	public static double GetShotAngleStdErr(double ability, boolean volley) {
		//return volley ? 0.63 - ability * 0.021 : 0.35 - ability * 0.014;
		return volley ? 0.60 - ability * 0.02 : 0.24 - ability * 0.01;
	}
	
	public static double GetShotVeritcalAngleStdErr(double ability, boolean volley) {
		//return volley ? 0.85 - ability * 0.024 : 0.35 - ability * 0.012;
		return volley ? 0.80 - ability * 0.02 : 0.20 - ability * 0.008;
	}
	
	public static double ShotOnTargetProbability(
			PlayerState player, Point position, boolean volley, boolean interfered) {
		double distance = PitchUtil.GetShotDistance(position, player.team_index);
		double angle = PitchUtil.GetShotAngle(position, player.team_index);
		double ability = GameUtil.GetShotAbility(player, distance, volley, interfered);
		double vertical_angle = PitchUtil.GetShotVerticalAngle(distance) / 2;
		return RandomUtil.NormalDistributionCDF(0, GetShotAngleStdErr(ability, volley), -angle / 2, angle / 2) *
				RandomUtil.NormalDistributionCDF(0, GetShotVeritcalAngleStdErr(ability, volley),
						-vertical_angle, vertical_angle);
	}
	
	public static boolean PenaltyKickOnTarget(double shot_ability, boolean shootout) {
		return !RandomUtil.WhetherToHappend(1.0 / (4.0 + 0.2 * shot_ability + (shootout ? 0.0 : 1.0)));
	}
	
	public static boolean PenaltySave(double shot_ability, double gk_ability) {
		return RandomUtil.WhetherToHappend((Math.max(0, gk_ability - shot_ability) * 0.1 + 1.0) / 8.0);
	}
	
	public static void GetQualifiedPlayers(
			TeamInfo team, Collection<PlayerState> selection, Predicate<PlayerState> qualifier) {
		selection.clear();
		for (PlayerState player : team.players) {
			if (qualifier.test(player)) selection.add(player);
		}
	}
	
	public static PlayerState GetMaxPlayer(TeamInfo team, Ability.Type ability_type, Predicate<PlayerState> filter) {
		PlayerState ret = null;
		double max_ability = Double.MIN_VALUE;
		for (PlayerState player : team.players) {
			if (filter.test(player)) continue;
			double ability = player.GetAbility(ability_type);
			if (ability > max_ability) {
				ret = player;
				max_ability = ability;
			}
		}
		return ret;
	}
	
	private static Map<ActionType, Double> kActionEnergyUsages = new TreeMap<ActionType, Double>() {
		private static final long serialVersionUID = 1L;{
		put(ActionType.DRIBBLE, 0.4);
		put(ActionType.PASS, 0.1);
		put(ActionType.SHOT, 0.4);
		put(ActionType.HEADER, 0.2);
		put(ActionType.PASS_CUT, 0.05);
		put(ActionType.WALK, 0.0075);
		put(ActionType.RUN, 0.015);
		put(ActionType.DASH, 0.03);
		put(ActionType.SAVE, 0.2);
	}};
	
	private static Map<ActionType, Integer> kActionDisabledFailure = new TreeMap<ActionType, Integer>() {
		private static final long serialVersionUID = 1L;{
		put(ActionType.DRIBBLE, 3);
		put(ActionType.PASS, 2);
		put(ActionType.SHOT, 4);
		put(ActionType.HEADER, 4);
		put(ActionType.PASS_CUT, 3);
		put(ActionType.TACKLE, 5);
		put(ActionType.SAVE, 5);
	}};
	
	private static Map<ActionType, Integer> kActionDisabledSuccuess = new TreeMap<ActionType, Integer>() {
		private static final long serialVersionUID = 1L;{
		put(ActionType.PASS, 2);
		put(ActionType.SHOT, 3);
		put(ActionType.HEADER, 2);
		put(ActionType.PASS_CUT, 1);
		put(ActionType.TACKLE, 1);
		put(ActionType.SAVE, 4);
	}};
}
