package fl.engine;

import java.util.ArrayList;
import java.util.List;
import fl.data.Constant;
import fl.data.GameParameter;
import fl.data.MatchResult;
import fl.data.PlayerInfo;
import fl.data.Position;
import fl.data.Ability;
import fl.data.Ability.Type;
import fl.engine.data.Game;
import fl.engine.data.Game.Action;
import fl.engine.data.Game.ActionType;
import fl.engine.data.Game.Directive;
import fl.engine.data.Game.MatchPhase;
import fl.engine.data.Game.State;
import fl.engine.data.MatchData;
import fl.engine.data.MatchInfo;
import fl.engine.data.MatchInfo.PlayerState;
import fl.engine.data.MatchInfo.TeamInfo;
import fl.engine.data.Pitch;
import fl.engine.data.PlayerData;
import fl.engine.utils.AbilityCalculator;
import fl.engine.utils.GameDecisionMaker;
import fl.engine.utils.GameLogger;
import fl.engine.utils.GameTimeUtil;
import fl.engine.utils.GameUtil;
import fl.engine.utils.InjuryUtil;
import fl.engine.utils.MatchRecorder;
import fl.engine.utils.PitchUtil;
import fl.utils.FieldAccessor;
import fl.utils.GeometryUtil;
import fl.utils.Line;
import fl.utils.Point;
import fl.utils.RandomUtil;
import fl.utils.ScoredObject;
import fl.utils.Vector;

public class MatchEngine {

	protected MatchEngine(MatchData match_data, MatchInfo match_info, GameLogger logger) {
		this.match_data = match_data;
		this.match_info = match_info;
		this.logger = logger;
		params = Constant.GetConstant().game_parameter;
		decision_maker = new GameDecisionMaker(match_info);
		recorder = new MatchRecorder(match_info, match_data);
	}
	
	public void Start() {
		recorder.RecordLineup();
		match_info.DoForAllPlayers(player -> {
			SetUpPlayer(player);
			PitchUtil.SetUpBasePosition(match_info.teams[player.team_index].formation, player);
		});
		MatchStart();
		match_info.DoForAllPlayers(recorder::RecordPlayerOff);
	}
	
	public void MatchStart() {
		MatchPhase[] phases = match_data.GetMatch().knockout ? Game.kKnockOutGame : Game.kNormalGame;
		int home_goals = 0;
		int away_goals = 0;
		for (int i = 0; i < phases.length; ++i) {
			if (i == 2) {
				match_data.GetResult().extra_time = true;
				match_info.teams[0].subsitition_chances++;
				match_info.teams[1].subsitition_chances++;
			}
			ResetKickOff(phases[i]);
			Proceed();
			home_goals = match_info.teams[0].first_leg_goals + match_data.GetResult().home.stats.goals;
			away_goals = match_info.teams[1].first_leg_goals + match_data.GetResult().away.stats.goals;
			if (directive.state == Game.State.ABANDON ||
					(match_data.GetMatch().knockout && (i == 1 || i == 3) && home_goals != away_goals)) {
				return;
			}
		}
		if (!match_data.GetMatch().knockout) return;
		match_info.phase = MatchPhase.PENALTY_KICK;
		match_info.time = 0;
		Shootouts();
	}
	
	private void ResetKickOff(MatchPhase phase) {
		match_info.phase = phase;
		match_info.time = 0;
		directive = new Game.Directive(Game.State.KICK_OFF,
				phase == MatchPhase.FIRST_HALF || phase == MatchPhase.EXTRA_TIME_FIRST_HALF);
		double stamina_recovered = (phase == MatchPhase.SECOND_HALF) ? 10.0 :
			((phase == MatchPhase.EXTRA_TIME_FIRST_HALF) ? 5.0 : 0.0);
		match_info.DoForAllPlayers(player -> {
			player.stamina = Math.min(100, player.stamina + stamina_recovered);
		});
		
	}
	
	private void Proceed() {
		int time_past =  0;
		do {
			if (directive.state != Game.State.NORMAL) {
				time_past = ResetTheBall();
				continue;
			}
			if (time_past > 0) {
				Move(time_past);
				time_past = 0;
				continue;
			}
			if (match_info.status == Game.State.PENALTY_KICK) {
				match_info.status = Game.State.NORMAL;
				time_past = PenaltyKick();
			} else if (match_info.ball.ball_owner != null) {
				UpdateChallengers();
				Action action = decision_maker.ChooseAction(challengers);
				Boolean interfered = false;
				if (HandleChallenge(action, interfered)) {
					match_info.status = Game.State.NORMAL;
					if (action.type == ActionType.PASS) {
						PlayerAction(match_info.ball.ball_owner, action.type, false);
						time_past = Pass(action, interfered);
					} else if (action.type == ActionType.SHOT) {
						PlayerAction(match_info.ball.ball_owner, action.type, false);
						time_past = Shot(interfered);
					} else {  // DRIBBLE
						time_past = Dribble(action);
					}
				}
				if (directive.state == Game.State.ABANDON) break;
				GameTimeUtil.UpdateTimeFromState(directive, match_info);
			} else {
				GameTimeUtil.IncTime(match_info, 1);
				time_past = 1;
			}
		} while (!GameTimeUtil.IsTimeOver(match_info) || directive.state == State.PENALTY_KICK ||
				match_info.status == Game.State.PENALTY_KICK);
		logger.LogEnd();
		logger.LogScore(match_data.GetResult());
	}
	
	private int ResetTheBall() {
		logger.LogBallReset(directive);
		recorder.RecordSetPiece(directive);
		{  // Don't record set piece time
			match_info.ball.owner_team = -1;
			recorder.ExchangeControl();
		}
		int time_past = ChangePlayers();
		match_info.ball.owner_team = directive.owned_by_home ? 0 : 1;
		match_info.ball.competition = false;
		match_info.ball.volley = false;
		match_info.passer = null;
		match_info.assistant = null;
		match_info.DoForAllPlayers(player -> { player.disabled = 0; });
		switch (directive.state) {
		case KICK_OFF:
			SetKickOffPosition();
			break;
		case THROW_IN:
			time_past += SetThrowIn();
			break;
		case GOAL_KICK:
			SetGoalKickPosition();
			break;
		case CORNER_KICK:
			SetCornerKickPosition();
			break;
		case FREE_KICK:
			SetFreeKickPosition();
			break;
		case PENALTY_KICK:
			SetPenaltyKickPosition();
			break;
		default:
			System.err.println(directive.state + " not supported yet.");
			System.exit(0);
		}
		directive = Game.kPlayOnDirective;
		return time_past;
	}
	
	private void UpdateChallengers() {
		challengers.clear();
		if (match_info.status != Game.State.NORMAL) return;
		GameUtil.GetQualifiedPlayers(match_info.DefenderTeam(), challengers,
				player -> GeometryUtil.Distance(match_info.ball.ball_owner.position, player.position)
					< params.max_distance_to_challenge && player.disabled <= 0);
	}
	
	private boolean HandleChallenge(Action action, Boolean interfered) {
		PlayerState new_ball_owner = null;
		boolean challenge_success = false;
		ActionType action_type = match_info.ball.volley ? ActionType.HEADER : action.type;
		int num_interfere = 0;
		for (PlayerState challenger : challengers) {
			Action challenge_action = this.decision_maker.ChooseChallengeAction(challenger);
			if (challenge_action.type == ActionType.IDLE) continue;
			if (challenger.pos == Position.GoalKeeper) {
				if (challengers.size() > 1 || RandomUtil.WhetherToHappend(0.5))  continue;
			}
			GameTimeUtil.IncTime(match_info, 1);
			interfered = true;
			double offset = match_info.ball.competition ? 0 : -1;
			if (action_type == ActionType.DRIBBLE) {
				offset = challenge_action.type == ActionType.TACKLE ? 0.5 : -0.5;
			} else if (action_type == ActionType.PASS) {
				offset = challenge_action.type == ActionType.PASS_CUT ? 0.5 : -0.5;
			}
			offset += (num_interfere++);
			challenge_success = GameUtil.Win(GameUtil.GetChallengeAbilityDelta(
					match_info.ball.ball_owner, challenger, action_type, offset, match_info.ball.competition));
			if (action_type == ActionType.DRIBBLE) {
				if (!match_info.ball.competition) {
					PlayerAction(match_info.ball.ball_owner, ActionType.DRIBBLE, !challenge_success);
					if (!challenge_success) logger.LogDribble(challenger);
				} else if (challenge_success) {
					GameUtil.UpdateDisabled(match_info.ball.ball_owner, ActionType.DRIBBLE, false);
				}
			}
			if (challenge_success) {
				boolean challenger_gets_ball = !match_info.ball.volley && RandomUtil.WhetherToHappend(0.4);
				if (action_type == ActionType.HEADER && challenger.pos != Position.GoalKeeper) {
					PlayerAction(challenger, ActionType.HEADER, true);
				} else if (!match_info.ball.competition) {
					PlayerAction(challenger, ActionType.TACKLE, challenger_gets_ball);
				}
				logger.LogDefendAction(challenger, challenge_action.type, challenger_gets_ball);
				if (!challenger_gets_ball) {
					if (!match_info.ball.competition) recorder.RecordClearance(challenger);
					GameUtil.BallMoved(match_info.ball.ball_owner.position, challenger.position,
							params.min_challenge_offset, params.max_challenge_offset, match_info.ball.position);
					directive = PitchUtil.CheckBoundary(match_info.ball.ball_owner.position,
							match_info.ball.position, 1 - match_info.ball.owner_team);
					if (directive.state != Game.State.NORMAL) logger.LogOutOfBoundary();
				} else {
					new_ball_owner = challenger;
				}
				break;
			} else {
				if (action_type != ActionType.HEADER || challenger.pos != Position.GoalKeeper) {
					PlayerAction(challenger, challenge_action.type, false);
				} else {
					GameUtil.UpdateDisabled(challenger, challenge_action.type, false);
				}
				boolean in_penalty_area = PitchUtil.IsPenalty(
						match_info.ball.ball_owner.position, match_info.ball.owner_team);
				boolean foul = (challenge_action.type == ActionType.TACKLE) &&
						RandomUtil.WhetherToHappend((match_info.ball.competition ?
								params.competition_foul_probability : params.tackle_foul_probability)
						* (in_penalty_area ? params.box_foul_factor : 1.0));
				if (foul) {
					logger.LogFoul(challenger);
					recorder.RecordFoul(challenger);
					if (in_penalty_area) {
						directive = new Game.Directive(Game.State.PENALTY_KICK, match_info.ball.owner_team == 0);
						match_info.assistant = match_info.ball.ball_owner;
					} else {
						directive = new Game.Directive(Game.State.FREE_KICK,
								match_info.ball.owner_team == 0, new Point(match_info.ball.ball_owner.position));
					}
					boolean injured = HandleInjury(true);
					boolean has_card = recorder.GetPlayerStats(challenger).yellow > 0;
					double card_probability_multiplier = (injured ? params.injury_card_factor : 1.0)
							* (has_card ? params.second_card_factor : 1.0);
					if (RandomUtil.WhetherToHappend(params.card_probablity * card_probability_multiplier)) {
						boolean red = RandomUtil.WhetherToHappend(params.red_card_probablity);
						logger.LogBooked(challenger, red, has_card);
						recorder.RecordBook(challenger, red);
						if (red || has_card) {
							recorder.RecordPlayerOff(challenger);
							if (match_info.DefenderTeam().players.length <= 7) {
								directive = Game.kMatchOverDirective;
								logger.LogAbandon(match_info.ball.owner_team == 1);
								recorder.AbandonGame(1 - match_info.ball.owner_team);
							} else {
								GameDecisionMaker.Transform(match_info.DefenderTeam(), challenger.player_index);
								for (PlayerState player : match_info.DefenderTeam().players) {
									UpdatePlayerAbility(player);
								}
							}
						}
					}
					recorder.ExchangeControl();
					return false;
				}
				HandleInjury(false);
			}
 		}
		if (action.type == ActionType.HEADER) {
			PlayerAction(match_info.ball.ball_owner, ActionType.HEADER, !challenge_success);
		}
		if (challenge_success) ExchangeControl(new_ball_owner);
		match_info.ball.competition = false;
		return !challenge_success;
	}
	
	private int Pass(Action action, boolean interfered) {
		logger.LogPass(action);
		match_info.ball.volley = action.volley_ball;
		PlayerState receiver = match_info.AttackTeam().players[action.player_index];
		Point start = match_info.ball.ball_owner.position;
		Point end = receiver.position;
		Vector path = new Vector(start, end);
		double path_distance = path.Length();
		Line passing_path = GeometryUtil.CreateLine(start, end);
		pass_cutters.clear();
		for (PlayerState player : match_info.DefenderTeam().players) {
			if (player.disabled > 0 || player.stamina <= 0.0 || player.pos == Position.GoalKeeper) continue;
			if (GeometryUtil.Distance(passing_path, player.position) < params.max_passcut_distance) {
				Vector v = new Vector(start, player.position);
				double distance = v.Length() < PitchUtil.kEpislon ? 0.0 : GeometryUtil.ProjectedLength(path, v);
				if (distance < 0 || distance > path_distance) continue;
				if (action.volley_ball && (distance > params.max_volley_passcut_distance ||
						distance < path_distance - params.max_volley_passcut_distance)) continue;
				pass_cutters.add(new ScoredObject<PlayerState>(player, distance));
			}
		}
		PlayerState intecepter = null;
		if (!pass_cutters.isEmpty()) {
			pass_cutters.sort(ScoredObject.SortByScoreAscending());
			double pass_ability = match_info.ball.ball_owner.GetAbility(Type.PASS) - (interfered ? 2 : 0);
			for (ScoredObject<PlayerState> pass_cutter : pass_cutters) {
				PlayerState player = pass_cutter.element;
				boolean cut_success = GameUtil.Win(GameUtil.GetPassCutAbility(player, action.volley_ball),
						pass_ability + 3.0);
				if (cut_success) {
					intecepter = player;
					int time_past = GameUtil.PassTime(pass_cutter.score);
					Vector v = new Vector(start, player.position);
					boolean cutter_gets_ball = RandomUtil.WhetherToHappend(0.4);
					//cutter_gets_ball = true;  // remove
					Point projected_point = new Point(start);
					if (v.Length() > PitchUtil.kEpislon) {
						projected_point.Add(GeometryUtil.GetProjectedVector(path, v));
					}
					GameTimeUtil.IncTime(match_info, time_past);
					PlayerAction(player, ActionType.PASS_CUT, cutter_gets_ball);
					logger.LogDefendAction(player, ActionType.PASS_CUT, cutter_gets_ball);
					int last_touch_team = 1 - match_info.ball.owner_team;
					ExchangeControl(cutter_gets_ball ? player : null);
					if (!cutter_gets_ball) {
						//recorder.RecordClearance(intecepter);
						GameUtil.BallMoved(projected_point, intecepter.position,
								params.min_challenge_offset, params.max_challenge_offset, match_info.ball.position);
						directive = PitchUtil.CheckBoundary(projected_point,
								match_info.ball.position, last_touch_team);
						if (directive.state != Game.State.NORMAL) logger.LogOutOfBoundary();
					}
					intecepter.position.Copy(projected_point);
					return time_past;
				}
				PlayerAction(player, ActionType.PASS_CUT, false);
			}
		}
		int time_past = GameUtil.PassTime(path_distance);
		GameTimeUtil.IncTime(match_info, time_past);
		boolean pass_hit_target = GameUtil.PassHitTarget(match_info.ball.ball_owner, path_distance);
		// pass_hit_target = true;  //remove
		if (pass_hit_target) {
			recorder.RecordAction(match_info.ball.ball_owner, ActionType.PASS, true);
			match_info.passer = match_info.ball.ball_owner;
			match_info.ball.ball_owner = receiver;
			match_info.ball.volley = action.volley_ball;
			match_info.ball.competition = true;
			logger.LogPassReseived(receiver);
		} else {
			GameUtil.BallOffTarget(end, params.min_pass_offset, params.max_pass_offset, path_distance,
					match_info.ball.position);
			// System.err.println("off ball - " + match_info.ball.position);
			directive = PitchUtil.CheckBoundary(start, match_info.ball.position, match_info.ball.owner_team);
			if (directive.state != Game.State.NORMAL) logger.LogOutOfBoundary();
			ExchangeControl(null);
		}
		return time_past;
	}
	
	private int Dribble(Action action) {
		match_info.ball.volley = false;
		GameTimeUtil.IncTime(match_info, 1);
		MoveToPosition(match_info.ball.ball_owner, action.target, 1, true);
		logger.LogRun();
		return 1;
	}
	
	private int Shot(boolean interfered) {
		logger.LogShot();
		if (match_info.passer != null) {
			recorder.RecordKeyPass(match_info.passer);
			match_info.assistant = match_info.passer;
		}
		PlayerState shooter = match_info.ball.ball_owner;
		Point start = shooter.position;
		Point end = Pitch.kGoalCenterPoint[1 - match_info.ball.owner_team];
		Vector path = new Vector(start, end);
		double path_distance = path.Length();
		Line shoting_path = GeometryUtil.CreateLine(start, end);
		double shot_ability = GameUtil.GetShotAbility(shooter, path_distance, match_info.ball.volley, interfered);
		shot_blockers.clear();
		for (PlayerState player : match_info.DefenderTeam().players) {
			if (player.disabled > 0 || player.stamina <= 0.0 || player.pos == Position.GoalKeeper) continue;
			if (GeometryUtil.Distance(shoting_path, player.position) < params.max_shotblock_distance) {
				Vector v = new Vector(start, player.position);
				double distance = v.Length() < PitchUtil.kEpislon ? 0.0 : GeometryUtil.ProjectedLength(path, v);
				if (distance < 0 || distance > path_distance) continue;
				shot_blockers.add(new ScoredObject<PlayerState>(player, distance));
			}
		}
		if (!shot_blockers.isEmpty()) {
			shot_blockers.sort(ScoredObject.SortByScoreAscending());
			for (ScoredObject<PlayerState> shot_blocker : shot_blockers) {
				PlayerState player = shot_blocker.element;
				boolean block_success = GameUtil.Win(GameUtil.GetShotBlockAbility(player) + 2, shot_ability);
				PlayerAction(player, ActionType.TACKLE, false);
				if (block_success) {
					int time_past = GameUtil.ShotTime(shot_blocker.score);
					Vector v = new Vector(start, player.position);
					Point projected_point = new Point(start);
					if (v.Length() > PitchUtil.kEpislon) {
						projected_point.Add(GeometryUtil.GetProjectedVector(path, v));
					}
					GameTimeUtil.IncTime(match_info, time_past);
					logger.LogDefendAction(player, ActionType.TACKLE, false);
					int last_touch_team = 1 - match_info.ball.owner_team;
					ExchangeControl(null);
					GameUtil.BallMoved(projected_point, player.position,
								params.min_challenge_offset, params.max_challenge_offset, match_info.ball.position);
					directive = PitchUtil.CheckBoundary(projected_point, match_info.ball.position, last_touch_team);
					if (directive.state != Game.State.NORMAL) logger.LogOutOfBoundary();
					return time_past;
				}
				
			}
		}
		int time_past = GameUtil.PassTime(path_distance);
		GameTimeUtil.IncTime(match_info, time_past);
		PlayerState gk = match_info.DefenderTeam().players[0];
		boolean on_target = RandomUtil.WhetherToHappend(GameUtil.ShotOnTargetProbability(
				shooter, shooter.position, match_info.ball.volley, interfered));
		boolean save_success = GameUtil.GoalSave(GameUtil.GetGKAbility(gk, path_distance, match_info.ball.volley) -
				shot_ability);
		boolean gk_gets_ball = save_success && GameUtil.Win(gk.GetAbility(Type.GOALKEEP), shot_ability);
		ShotResult(on_target, save_success, gk_gets_ball, false);
		return time_past;
	}
	
	private int PenaltyKick() {
		logger.LogShot();
		PlayerAction(match_info.ball.ball_owner, ActionType.SHOT, false);
		double shot_ability = match_info.ball.ball_owner.GetAbility(Type.SHOT);
		double gk_ability = match_info.DefenderTeam().players[0].GetAbility(Type.GOALKEEP);
		boolean on_target = GameUtil.PenaltyKickOnTarget(shot_ability, false);
		boolean save_success = GameUtil.PenaltySave(shot_ability, gk_ability);
		boolean gk_gets_ball = RandomUtil.WhetherToHappend(0.1);
		ShotResult(on_target, save_success, gk_gets_ball, true);
		return 2;
	}
	
	private boolean ShootoutPenaltyKick(PlayerState kicker, PlayerState gk) {
		double shot_ability = kicker.GetAbility(Type.SHOT);
		double gk_ability = gk.GetAbility(Type.GOALKEEP);
		boolean on_target = GameUtil.PenaltyKickOnTarget(shot_ability, true);
		boolean save_success = GameUtil.PenaltySave(shot_ability, gk_ability);
		logger.LogShootoutPenalty(kicker, gk, on_target, save_success);
		return on_target && !save_success;
	}
	
	private void ShotResult(boolean on_target, boolean save_success, boolean gk_gets_ball, boolean penalty) {
		PlayerState shooter = match_info.ball.ball_owner;
		PlayerState gk = match_info.DefenderTeam().players[0];
		boolean is_left = RandomUtil.WhetherToHappend(0.5);
		if (on_target) {
			PlayerAction(shooter, ActionType.SHOT, true);
			PlayerAction(gk, ActionType.SAVE, save_success);
			logger.LogGoalSaving(save_success, gk_gets_ball);
			if (save_success) {
				if (gk_gets_ball) {
					ExchangeControl(gk);
				} else if (RandomUtil.WhetherToHappend(params.corner_kick_probability)) {
					logger.LogOutOfBoundary();
					directive = new Game.Directive(Game.State.CORNER_KICK, match_info.ball.owner_team == 0, is_left);
					ExchangeControl(null);
				} else {
					GameUtil.BallMoved(Pitch.kGoalCenterPoint[1 - match_info.ball.owner_team],
							new Vector(0, match_info.ball.owner_team == 0 ? -1 : 1),
							params.min_challenge_offset, params.max_challenge_offset, Math.PI / 3,
							match_info.ball.position);
					match_info.assistant = shooter;
					ExchangeControl(null);
				}
			} else {
				recorder.RecordGoal(penalty);
				logger.LogGoal();
				logger.LogScore(match_data.GetResult());
				directive = new Game.Directive(Game.State.KICK_OFF, match_info.ball.owner_team == 1);
				ExchangeControl(null);
			}
		} else if (RandomUtil.WhetherToHappend(params.shot_on_post_probability)){
			GameUtil.BallMoved(Pitch.kPost[1 - match_info.ball.owner_team][is_left ? 0 : 1],
					new Vector(0, match_info.ball.owner_team == 0 ? -1 : 1),
					params.min_challenge_offset, params.max_challenge_offset, Math.PI / 3,
					match_info.ball.position);
			match_info.assistant = shooter;
			ExchangeControl(null);
			logger.LogShotOnPost();
		} else {
			logger.LogOutOfBoundary();
			directive = new Game.Directive(Game.State.GOAL_KICK, match_info.ball.owner_team == 1, is_left);
			ExchangeControl(null);
		}
	}
	
	private void Move(int time_past) {
		Point ball_position = match_info.ball.ball_owner == null ?
				match_info.ball.position : match_info.ball.ball_owner.position;
		boolean competing = (match_info.ball.ball_owner == null);
		PlayerState temp_owner = null;
		double max_remaining_time = -1;
		for (int i = 0; i < 2; ++i) {
			TeamInfo team = match_info.teams[i];
			PlayerState nearest_player = null;
			boolean own_ball = (i == match_info.ball.owner_team);
			if (!own_ball) {
				nearest_player = PitchUtil.FindNearestPlayer(team, ball_position);
			}
			for (PlayerState player : match_info.teams[i].players) {
				int move_sec = GameUtil.UpdateDisabled(player, time_past);
				if (move_sec <= 0 || player == match_info.ball.ball_owner) continue;
				if (player == nearest_player ||
						(!own_ball && GeometryUtil.Distance(ball_position, player.position) <
								params.max_distance_to_compete_ball && player.pos != Position.GoalKeeper)) {
					double remaining_time = MoveToPosition(player, ball_position, time_past, true);
					if (competing && remaining_time > max_remaining_time) {
						max_remaining_time = remaining_time;
						temp_owner = player;
					}
				} else {
					MoveToPosition(player, 
							PitchUtil.TargetPosition(player, ball_position, match_info.ball.owner_team),
							time_past, false);
				}
			}
		}
		if (competing && temp_owner != null) {
			match_info.ball.ball_owner = temp_owner;
			match_info.ball.owner_team = temp_owner.team_index;
			match_info.ball.competition = true;
			logger.LogGetBall(temp_owner);
		}
	}
	
	private void PlayerAction(PlayerState player, ActionType action_type, boolean success) {
		recorder.RecordAction(player, action_type, success);
		GameUtil.UpdateStamina(player, action_type, 1);
		GameUtil.UpdateDisabled(player, action_type, success);
	}
	
	private double MoveToPosition(PlayerState player, Point p, int time, boolean dash) {
		Vector dir = new Vector(player.position, p);
		double distance = dir.Length();
		if (distance < 1.0) return 0;
		double speed = GameUtil.GetSpeed(player);
		double ratio = speed * time / distance;
		dash = dash && player.stamina > 20 && (ratio < 1.25);
		GameUtil.UpdateStamina(player,
				dash ? ActionType.DASH : (ratio > 4 ? ActionType.WALK : ActionType.RUN), time);
		speed *= (dash ? 1.0 : 0.8);
		double moved_distance = Math.min(speed * time, distance);
		player.position.Add(dir.Scale(moved_distance));
		return (speed * time >= distance) ? (time - moved_distance / speed) : -1;
	}
	
	private void ExchangeControl(PlayerState player) {
		recorder.ExchangeControl();
		match_info.ball.ball_owner = player;
		match_info.ball.owner_team = player != null ? player.team_index : -1;
		match_info.passer = null;
		if (match_info.assistant != null && match_info.assistant.team_index != match_info.ball.owner_team ) {
			match_info.assistant = null;
		}
	}
	
	private void SetKickOffPosition() {
		match_info.DoForAllPlayers(PitchUtil::SetUpKickoffPosition);
		TeamInfo attacker = match_info.AttackTeam();
		match_info.ball.ball_owner = attacker.players[attacker.FormationSetting().kickoff_kicker];
		match_info.ball.ball_owner.position.Set(Pitch.kHalfWidth, Pitch.kHalfLength);
		match_info.status = Game.State.KICK_OFF;
	}
	
	private void SetGoalKickPosition() {
		match_info.DoForAllPlayers(PitchUtil::SetUpGoalKickPosition);
		match_info.ball.ball_owner = match_info.AttackTeam().players[0];
		match_info.status = Game.State.GOAL_KICK;
	}
	
	private void SetCornerKickPosition() {
		match_info.DoForAllPlayers(player -> {
			PitchUtil.SetUpCornerKickPosition(player, match_info.teams[player.team_index].formation,
					match_info.ball.owner_team);
		});
		PlayerState ball_owner = GameUtil.GetMaxPlayer(match_info.AttackTeam(), Type.PASS, GameUtil::IsGoalKeeper);
		match_info.ball.ball_owner = ball_owner;
		ball_owner.position.Set(directive.is_left ? 0 : Pitch.kWidth, directive.owned_by_home ? Pitch.kLength : 0);
		PitchUtil.CapBoundary(ball_owner.position);
		match_info.status = Game.State.CORNER_KICK;
	}
	
	private void SetFreeKickPosition() {
		match_info.DoForAllPlayers(player -> {
			player.position.Copy(PitchUtil.TargetPosition(player, directive.position, match_info.ball.owner_team));
		});
		PlayerState kicker = decision_maker.FreeKickOwner(directive.position);
		kicker.position.Copy(directive.position);
		decision_maker.ArrangeWall(directive.position);
		match_info.status = Game.State.FREE_KICK;
	}
	
	private void SetPenaltyKickPosition() {
		match_info.DoForAllPlayers(player -> {
			PitchUtil.SetUpPenaltyKickPosition(player, match_info.ball.owner_team);
		});
		match_info.ball.ball_owner = decision_maker.PenaltyKickOwner();
		match_info.status = Game.State.PENALTY_KICK;
	}
	
	private int SetThrowIn() {
		ScoredObject<PlayerState> ball_owner = ScoredObject.GetMin(match_info.AttackTeam().players,
				player -> { return GeometryUtil.Distance(directive.position, player.position);},
				GameUtil::IsGoalKeeper);
		match_info.ball.ball_owner = ball_owner.element;
		match_info.status = Game.State.NORMAL;
		return (int) (ball_owner.score / GameUtil.GetSpeed(ball_owner.element));
	}
	
	private void Shootouts() {
		List<PlayerState> home_kickers = decision_maker.ShootoutKickers(match_info.teams[0]);
		List<PlayerState> away_kickers = decision_maker.ShootoutKickers(match_info.teams[1]);
		int[] remaining_turns = { 5, 5 };
		int[] goals = { 0, 0 };
		int kick_team = RandomUtil.WhetherToHappend(0.5) ? 0 : 1;
		int round = 0;
		while(goals[0] <= goals[1] + remaining_turns[1] && goals[1] <= goals[0] + remaining_turns[0]) {
			if (remaining_turns[0] == 0 && remaining_turns[1] == 0) {
				remaining_turns[0] = 1;
				remaining_turns[1] = 1;
			}
			remaining_turns[kick_team]--;
			List<PlayerState> kickers = kick_team == 0 ? home_kickers : away_kickers;
			boolean goal = ShootoutPenaltyKick(kickers.get((round / 2) % kickers.size()),
					match_info.teams[1 - kick_team].players[0]);
			if (goal) goals[kick_team]++;
			kick_team = 1 - kick_team;
			round++;
		}
		MatchResult result = match_data.GetResult();
		result.home.stats.shootout_goals = goals[0];
		result.away.stats.shootout_goals = goals[1];
		logger.LogScore(result);
	}
	
	private boolean HandleInjury(boolean foul) {
		PlayerState player = match_info.ball.ball_owner;
		if (InjuryUtil.Injured(foul ? (match_info.ball.competition ?
				params.competition_injury_probability : params.tackle_injury_probability) :
				params.no_foul_injury_probability, player.player)) {
			player.disabled = Integer.MAX_VALUE;
			player.injury = InjuryUtil.InjuryTime();
			logger.LogInjury();
			return true;
		}
		return false;
	}
	
	private int ChangePlayers() {
		int player_changed = 0;
		for (int i = 0; i < 2; ++i) {
			TeamInfo team = match_info.teams[i];
			if (team.subsitition_chances == 0 || team.substitues.isEmpty()) continue;
			for (PlayerState player : team.players) {
				if (GameDecisionMaker.MustSubstitute(player, team)) {
					ChangePlayer(player, team);
					player_changed++;
					if (team.subsitition_chances == 0 || team.substitues.isEmpty()) break;
				}
			}
			team.formation_changed = false;
			decision_maker.ShouldSubstitue(i, players_to_replace);
			for (PlayerState player : players_to_replace) {
				if (ChangePlayer(player, team)) player_changed++;
			}
		}
		return GameTimeUtil.SubstitutionTime(player_changed);
	}
	
	private boolean ChangePlayer(PlayerState player, TeamInfo team) {
		PlayerData substitute = GameDecisionMaker.PickOneSubstitute(player.pos, team.substitues);
		if (substitute == null) return false;
		logger.LogSubstitution(player, substitute);
		recorder.RecordSubstitution(player, substitute);
		player.player = substitute;
		SetUpPlayer(player);
		team.substitues.remove(substitute);
		team.subsitition_chances--;
		return true;
	}
	
	private void SetUpPlayer(PlayerState player) {
		PlayerInfo player_info = player.player.GetInfo();
		player.stamina = FieldAccessor.kHundredBased.Get(player_info.stamina);
		player.injury = 0;
		player.disabled = 0;
		UpdatePlayerAbility(player);
	}
	
	private void UpdatePlayerAbility(PlayerState player) {
		PlayerInfo player_info = player.player.GetInfo();
		player.ability = new Ability();
		AbilityCalculator.UpdateAbility(player_info, player.pos, player.ability);
		if (!match_data.GetMatch().neutral_site && player.team_index == 0) {
			player.ability_multiplier = 1.025;
		}
	}

	private Directive directive;
	private MatchData match_data;
	private MatchInfo match_info;
	
	private GameParameter params;
	private GameDecisionMaker decision_maker;
	private MatchRecorder recorder;
	private GameLogger logger;
	
	private List<ScoredObject<PlayerState>> pass_cutters = new ArrayList<ScoredObject<PlayerState>>();
	private List<ScoredObject<PlayerState>> shot_blockers = new ArrayList<ScoredObject<PlayerState>>();
	private List<PlayerState> challengers = new ArrayList<PlayerState>();
	private List<PlayerState> players_to_replace = new ArrayList<PlayerState>();
}
