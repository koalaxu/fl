package fl.engine.utils;

import fl.engine.data.MatchInfo;

import fl.engine.data.Pitch;
import fl.engine.data.PlayerData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import fl.data.Position;
import fl.data.Ability.Type;
import fl.data.Position.Location;
import fl.data.Position.PositionCategory;
import fl.engine.data.Game;
import fl.engine.data.Game.Action;
import fl.engine.data.Game.ActionType;
import fl.engine.data.Game.MatchPhase;
import fl.engine.data.MatchInfo.PlayerState;
import fl.engine.data.MatchInfo.TeamInfo;
import fl.utils.GeometryUtil;
import fl.utils.Point;
import fl.utils.RandomUtil;
import fl.utils.ScoredObject;
import fl.utils.Vector;
import fl.utils.WeightedSampler;

public class GameDecisionMaker {
	public GameDecisionMaker(MatchInfo match) {
		this.match = match;
	}
	
	public Action ChooseAction(Collection<PlayerState> challengers) {
		if (match.status == Game.State.KICK_OFF || match.status == Game.State.CORNER_KICK ||
				match.status == Game.State.GOAL_KICK) {
			return MakePass();
		}
		if (match.status != Game.State.FREE_KICK && WhetherToDribble()) {
			return MakeDribble();
		}
		if (WhetherToShot(!challengers.isEmpty())) {
			return MakeAction(ActionType.SHOT);
		}
		return MakePass();
	}
	
	public Action ChooseChallengeAction(PlayerState challenger) {
		if (challenger.stamina > 0) {
			if (match.ball.volley) return MakeAction(ActionType.HEADER);
			if (RandomUtil.WhetherToHappend(0.5)) return MakeAction(ActionType.TACKLE);
			return MakeAction(ActionType.PASS_CUT);
		}
		return MakeAction(ActionType.IDLE);
	}
	
	public PlayerState FreeKickOwner(Point freekick_position) {
		PlayerState potential_kicker = GameUtil.GetMaxPlayer(match.AttackTeam(), Type.SHOT, GameUtil::IsGoalKeeper);
		if (GameUtil.ShotOnTargetProbability(potential_kicker, freekick_position, false, false) > 0.5)
			return potential_kicker;
		return match.ball.ball_owner;
	}
	
	public PlayerState PenaltyKickOwner() {
		return GameUtil.GetMaxPlayer(match.AttackTeam(), Type.SHOT, GameUtil::IsGoalKeeper);
	}
	
	public List<PlayerState> ShootoutKickers(TeamInfo team) {
		List<ScoredObject<PlayerState>> scored_list = new ArrayList<ScoredObject<PlayerState>>();
		List<PlayerState> list = new ArrayList<PlayerState>();
		ScoredObject.CreateScoredList(
				team.players, scored_list, player -> { return player.GetAbility(Type.SHOT); });
		scored_list.sort(ScoredObject.SortByScore());
		ScoredObject.ScoredListToList(scored_list, list);
		return list;
	}
	
	public void ArrangeWall(Point freekick_position) {
		Set<PlayerState>  wall = new HashSet<PlayerState>();
		for (PlayerState defender : match.DefenderTeam().players) {
			if (GeometryUtil.Distance(freekick_position, defender.position) < Pitch.kFreeKickDistance) {
				if (PitchUtil.RelativeDepth(freekick_position, match.ball.owner_team) < Pitch.kHalfLength) {
					defender.position.y += (match.ball.owner_team == 0 ? 1 : -1) * Pitch.kFreeKickDistance;
					if (!PitchUtil.InBoundary(defender.position)) wall.add(defender);
				} else {
					wall.add(defender);
				}
			}
		}
		double shot_distance = PitchUtil.GetShotDistance(freekick_position, match.ball.owner_team);
		int num_wall_players = kWallPlayers.length - 1;
		for (; num_wall_players >= 0; --num_wall_players) {
			if (kWallPlayers[num_wall_players] > shot_distance) break;
		}
		for (int i = 1; wall.size() < num_wall_players && i < match.DefenderTeam().players.length; ++i) {
			PlayerState defender = match.DefenderTeam().players[i];
			if (defender.pos.GetPositionCategory() == PositionCategory.DEFENDER ||
					wall.contains(defender)) continue;
			wall.add(defender);
		}
		if (wall.isEmpty()) return;
		Vector goal_direction = new Vector(freekick_position, PitchUtil.GetGoalPosition(match.ball.owner_team));
		Point wall_position = new Point(freekick_position);
		wall_position.Add(goal_direction.Scale(Pitch.kFreeKickDistance));
		for (PlayerState wall_player: wall) {
			wall_player.position.Copy(wall_position);
		}
	}
	
	public static boolean MustSubstitute(PlayerState player, TeamInfo team) {
		if (player.injury > 0) return true;
		if (team.formation_changed) {
			double base_score = 
					PositionAnalyzer.GetOverallScore(player.player.GetInfo(), player::GetAbility, player.pos);
			double max_score = ScoredObject.GetMax(team.substitues,
					sub -> AbilityCalculator.PlayerScore(sub.GetInfo(), player.pos), null).score;
			final double kMinDelta = 1.2;
			if (max_score > base_score * kMinDelta) return true;
		}
		return false;
	}
	
	public void ShouldSubstitue(int team_index, List<PlayerState> players_to_replace) {
		TeamInfo team = match.teams[team_index];
		players_to_replace.clear();
		int time = GameTimeUtil.GetAbsoluteTime(match) / 60;
		int chances = Math.min(team.substitues.size(), team.subsitition_chances - Math.max(0, (94 - time) / 10));
		if (chances <= 0 || match.phase == MatchPhase.FIRST_HALF) return;
		List<ScoredObject<PlayerState>> candidates = new ArrayList<ScoredObject<PlayerState>>();
		ScoredObject.CreateScoredList(team.players, candidates, player -> {
			Position position = player.pos;
			PlayerData candidate = PickOneSubstitute(position, team.substitues);
			if (candidate == null) return -1.0;
			return  AbilityCalculator.PlayerScore(candidate.GetInfo(), position) -
					AbilityCalculator.PlayerScore(player, position);
		});
		candidates.sort(ScoredObject.SortByScore());
		for (int i = 0; i < chances; ++i) {
			if (candidates.get(i).score <= 0.01) break;
			players_to_replace.add(candidates.get(i).element);
		}
	}
	
	public static PlayerData PickOneSubstitute(Position position, Collection<PlayerData> substitutes) {
		PlayerData candidate = ScoredObject.GetMax(substitutes,
				player -> AbilityCalculator.PlayerScore(player.GetInfo(), position),
				player -> player.GetInfo().GetPosition() != position
					&& player.GetInfo().GetSecondaryPosition() != position).element;
		if (candidate != null) return candidate;
		return ScoredObject.GetMax(substitutes,
				player -> AbilityCalculator.PlayerScore(player.GetInfo(), position), null).element;
	}
	
	public static void Transform(TeamInfo team, int sent_off_index) {
		FormationUtil.Transform transform = FormationUtil.transformations.get(team.formation);
		PlayerState[] old_squad = team.players;
		old_squad[sent_off_index] = old_squad[transform.dismissed_position];
		PlayerState[] new_squad = new PlayerState[old_squad.length - 1];
		for (int i = 0; i < new_squad.length; ++i) {
			new_squad[i] = (i < transform.dismissed_position) ? old_squad[i] : old_squad[i + 1];
			new_squad[i].player_index = i;
			new_squad[i].pos = FormationUtil.GetFormationPositions(transform.new_formation).get(i);
		}
		team.players = new_squad;
		team.formation = transform.new_formation;
		team.formation_changed = true;
	}
	
	private boolean WhetherToShot(boolean interfered) {
		if (match.ball.ball_owner.pos == Position.GoalKeeper) return false;
		double probability = GameUtil.ShotOnTargetProbability(
				match.ball.ball_owner, match.ball.ball_owner.position, match.ball.volley, interfered);
		if (probability < 0.1) return false;
		if (match.DefenderTeam().players[0].disabled > 1 && probability > 0.3) return true;
		boolean shot = RandomUtil.WhetherToHappend(1.0 - Math.pow(1.0 - probability, 0.9));
		return shot;
	}
	
	private boolean WhetherToDribble() {
		if (match.ball.volley) return false;
		double probability = kDribblePossibilities.getOrDefault(match.ball.ball_owner.pos, 0.0) *
				(PitchUtil.RelativeDepth(match.ball.ball_owner.position, match.ball.owner_team) < 0.5 ? 0.5 : 0.7);
		return RandomUtil.WhetherToHappend(probability);
	}
	
	private Action MakeDribble() {
		Action action = new Action(ActionType.DRIBBLE);
		action.target = PitchUtil.AttackingPosition(match.ball.ball_owner, match.ball.owner_team);
		return action;
	}
	
	private Action MakePass() {
		Action action = new Action(ActionType.PASS);
		// Choose Receiver
		PlayerState owner = match.ball.ball_owner;
		action.player_index = (match.status == Game.State.CORNER_KICK) ?
				ChooseReceiverForCorner() : ChooseReceiver();
		// Choose High/Low ball
		double distance = GeometryUtil.Distance(owner.position,
				match.AttackTeam().players[action.player_index].position);
		if (distance > Game.kLongPassDistance) {
			action.volley_ball = true;
		} else if (distance > Game.kMediumPassDistance) {
			action.volley_ball = RandomUtil.WhetherToHappend(0.5);
		}
		return action;
	}
	
	private int ChooseReceiver() {
		PlayerState owner = match.ball.ball_owner;
		Map<PositionCategory, Double> choice_prob = kPassChoicePossibilities.get(owner.pos.GetPositionCategory());
		WeightedSampler<Integer> choices = new WeightedSampler<Integer>();
		for (int i = 0; i < match.AttackTeam().players.length; ++i) {
			PlayerState player = match.AttackTeam().players[i];
			if (player == match.ball.ball_owner) continue;
			double prob_multiplier = 1.0;
			if (PitchUtil.RelativeDepth(owner.position, match.ball.owner_team) < 0.4 &&
					player.pos.GetPositionCategory().ordinal() <= owner.pos.GetPositionCategory().ordinal()) {
				prob_multiplier = 0.1;
			}
			choices.Add(i, choice_prob.getOrDefault(player.pos.GetPositionCategory(), 0.0)
				* (player.pos.location == Location.CENTER ? 1.0 : 0.5) * prob_multiplier);
		}
		return choices.Sample();
	}
	
	private int ChooseReceiverForCorner() {
		WeightedSampler<Integer> choices = new WeightedSampler<Integer>();
		for (int i = 0; i < match.AttackTeam().players.length; ++i) {
			PlayerState player = match.AttackTeam().players[i];
			if (player == match.ball.ball_owner || player.pos == Position.GoalKeeper) continue;
			double depth = PitchUtil.RelativeDepth(player.position, match.ball.owner_team);
			double weight = Math.pow(player.GetAbility(Type.JUMP), 2.);
			if (depth > 0.7) {
				choices.Add(i, weight);
			} else if (depth > 0.5) {
				choices.Add(i, 0.1 * weight);
			} else {
				choices.Add(i, 0.01 * weight);
			}
		}
		return choices.Sample();
	}
	
	private Action MakeAction(ActionType type) {
		return new Action(type);
	}
	
	private static Map<PositionCategory, Map<PositionCategory, Double>> kPassChoicePossibilities =
			new TreeMap<PositionCategory, Map<PositionCategory, Double>>() {
				private static final long serialVersionUID = 1L; {
		Map<PositionCategory, Double> gk_choice = new TreeMap<PositionCategory, Double>() {
			private static final long serialVersionUID = 1L; {
				put(PositionCategory.DEFENDER, 0.3);
				put(PositionCategory.MIDFIELDER, 0.1);
				put(PositionCategory.FORWARD, 0.1);
		}};
		put(PositionCategory.GOAL_KEEPER, gk_choice);
		Map<PositionCategory, Double> df_choice = new TreeMap<PositionCategory, Double>() {
			private static final long serialVersionUID = 1L; {
				put(PositionCategory.DEFENDER, 0.2);
				put(PositionCategory.MIDFIELDER, 0.3);
				put(PositionCategory.FORWARD, 0.1);
		}};
		put(PositionCategory.DEFENDER, df_choice);
		Map<PositionCategory, Double> mf_choice = new TreeMap<PositionCategory, Double>() {
			private static final long serialVersionUID = 1L; {
				put(PositionCategory.DEFENDER, 0.1);
				put(PositionCategory.MIDFIELDER, 0.3);
				put(PositionCategory.FORWARD, 0.2);
		}};
		put(PositionCategory.MIDFIELDER, mf_choice);
		Map<PositionCategory, Double> fw_choice = new TreeMap<PositionCategory, Double>() {
			private static final long serialVersionUID = 1L; {
				put(PositionCategory.MIDFIELDER, 0.2);
				put(PositionCategory.FORWARD, 0.3);
		}};
		put(PositionCategory.FORWARD, fw_choice);
	}};
	
	private static Map<Position, Double> kDribblePossibilities = new HashMap<Position, Double>() {
		private static final long serialVersionUID = 1L; {
		put(Position.GoalKeeper, 0.0);
		put(Position.Sweeper, 0.75); put(Position.CentreBack, 0.5);
		put(Position.LeftBack, 0.9375); put(Position.RightBack, 0.9375);
		put(Position.DefensiveMidFielder, 0.5);
		put(Position.CentralMidFielder, 0.9);
		put(Position.AttackingMidFielder, 0.9375);
		put(Position.LeftMidFielder, 0.96); put(Position.RightMidFielder, 0.96);
		put(Position.CentreForward, 0.96); put(Position.SecondStriker, 0.96);
		put(Position.LeftWinger, 0.9775); put(Position.RightWinger, 0.9775);
	}};
	
	private static int[] kWallPlayers = { Integer.MAX_VALUE, 120, 105, 90, 75, 60};
	
	private MatchInfo match;
}
