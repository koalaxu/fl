package fl.engine.utils;

import java.util.List;

import fl.data.Formation;
import fl.data.Position;
import fl.data.Position.Location;
import fl.data.Position.PositionCategory;
import fl.engine.data.Game;
import fl.engine.data.MatchInfo.PlayerState;
import fl.engine.data.MatchInfo.TeamInfo;
import fl.engine.data.Pitch;
import fl.engine.utils.FormationUtil.FormationSetting;
import fl.utils.GeometryUtil;
import fl.utils.LineSegment;
import fl.utils.Point;
import fl.utils.RandomUtil;
import fl.utils.Vector;

public class PitchUtil {	
	public static void SetUpBasePosition(Formation formation, PlayerState player) {
		List<Point> coordinates = FormationUtil.GetFormationCoordinates(formation);
		Point co = coordinates.get(player.player_index);
		player.base_position = new Point(co.x * Pitch.kWidth, co.y * Pitch.kHalfLength);
		if (player.team_index > 0) Rotate(player.base_position);
	}
	
	public static void SetUpKickoffPosition(PlayerState player) {
		player.position.Copy(player.base_position);
	}
	
	public static void SetUpGoalKickPosition(PlayerState player) {
		player.position.Copy(player.base_position);
		if (player.player_index == 0) return;
		player.position.y = player.position.y + (player.team_index == 0 ? 0.3 : -0.3) * Pitch.kHalfLength;
	}
	
	public static void SetUpCornerKickPosition(PlayerState player, Formation formation, int ball_owner_team) {
		player.position.Copy(player.base_position);
		if (player.player_index == 0) return;
		player.position.x = Pitch.kHalfWidth + (player.position.x - Pitch.kHalfWidth) * 0.75;
		if (player.team_index == ball_owner_team) {
			FormationSetting setting = FormationUtil.settings.get(formation);
			if (setting.corner_defenders.contains(player.player_index)) {
				player.position.y = Pitch.kHalfLength;
			} else if (player.pos == Position.CentreBack) {
				player.position.y = Pitch.kLength - 36;
			} else if (player.pos.GetPositionCategory() == PositionCategory.DEFENDER) {
				player.position.y = Pitch.kHalfLength;
			} else if (player.pos.GetPositionCategory() == PositionCategory.FORWARD) {
				player.position.y = Pitch.kLength - 20;
			} else {
				player.position.y = Pitch.kLength - 60;
			}
			
		} else if (player.team_index == 1 - ball_owner_team) {
			if (player.pos == Position.CentreForward) {
				player.position.y = 36;
			} else if (player.pos.GetPositionCategory() == PositionCategory.DEFENDER) {
				player.position.y = 20;
			} else if (player.pos.GetPositionCategory() == PositionCategory.FORWARD) {
				player.position.y = Pitch.kHalfLength - 20;
			} else {
				player.position.y = 56;
			}
		} else {
			System.err.println("Not owner corner");
			System.exit(0);
		}
		if (player.team_index > 0) Mirror(player.position);
	}
	
	public static void SetUpPenaltyKickPosition(PlayerState player, int ball_owner_team) {
		player.position.Copy(player.base_position);
		if (player.player_index == 0) return;
		player.position.y = ball_owner_team == 0 ? Pitch.kLength - Pitch.kBoxLength : Pitch.kBoxLength;
	}
	
	public static Point TargetPosition(PlayerState player, Point ball, int ball_owner_team) {
		Point target_position = new Point(player.base_position);
		Point ball_position = new Point(ball);
		if (player.pos == Position.GoalKeeper) return target_position;
		if (player.team_index > 0) {
			Rotate(target_position);
			Rotate(ball_position);
		}
		if (player.team_index == ball_owner_team) {  // Attacking
			target_position.y = target_position.y - 0.3 * Pitch.kHalfLength + ball_position.y;
		} else if (player.team_index == 1 - ball_owner_team) { // Defending
			target_position.y = target_position.y - 0.9 * Pitch.kHalfLength + ball_position.y;
			if (player.pos.GetPositionCategory() == PositionCategory.DEFENDER) {
				target_position.y = Math.max(target_position.y, kDefendLineInDefense);
			} else if (player.pos.GetPositionCategory() == PositionCategory.MIDFIELDER) {
				target_position.y = Math.max(target_position.y, kMidFieldLineInDefense);
			} else if (player.pos.GetPositionCategory() == PositionCategory.FORWARD) {
				target_position.y = Math.max(target_position.y, kAttackLineInDefense);
			}
		} else {
			target_position.y = target_position.y - 0.6 * Pitch.kHalfLength + ball_position.y;
		}
		if (player.pos.GetPositionCategory() == PositionCategory.DEFENDER) {
			target_position.y = Math.min(kDefendLine, Math.max(kDefendLineInDefense, target_position.y));
		} else if (player.pos.GetPositionCategory() == PositionCategory.MIDFIELDER) {
			target_position.y = Math.min(kMidFieldLine, Math.max(kMidFieldLineInDefense, target_position.y));
		} else if (player.pos.GetPositionCategory() == PositionCategory.FORWARD) {
			target_position.y = Math.min(kAttackLine, Math.max(kAttackLineInDefense, target_position.y));
		}
		Vector v = new Vector(target_position, ball_position);
		if (v.Length() > 0) {
			v.y = v.y / 2;
			target_position.Add(v.Scale(v.Length() * 0.1));
		}
		RandomPointinRadius(target_position, 0, 6, target_position);
		CapBoundary(target_position);
		if (player.team_index > 0) Rotate(target_position);
		return target_position;
	}
	
	public static Point AttackingPosition(PlayerState player, int ball_owner_team) {
		Point target_position = new Point(player.base_position);
		if (player.team_index > 0) Rotate(target_position);
		Position pos = player.pos;
		double player_position_y = player.team_index == 0 ? player.position.y : Pitch.kLength - player.position.y;
		if (pos.location == Location.LEFT || pos.location == Location.RIGHT ||
				pos.GetPositionCategory() == PositionCategory.FORWARD || pos == Position.AttackingMidFielder) {
			target_position.y = Pitch.kLength - 18;
		} else if (pos.GetPositionCategory() == PositionCategory.DEFENDER || pos == Position.DefensiveMidFielder) {
			target_position.y = Pitch.kLength - 60;
		} else if (pos.GetPositionCategory() == PositionCategory.MIDFIELDER) {
			target_position.y = Pitch.kLength - 36;
		}
		
		if (player_position_y - target_position.y > 5 && (pos.GetPositionCategory() == PositionCategory.MIDFIELDER
				|| pos.GetPositionCategory() == PositionCategory.FORWARD)) {
			target_position.x = Pitch.kHalfWidth;
			target_position.y = Pitch.kLength;
		}
	
		target_position.y = Math.max(target_position.y, player_position_y);
		RandomPointinRadius(target_position, 0, 6, target_position);
		CapBoundary(target_position);
		if (player.team_index > 0) Rotate(target_position);
		return target_position;
	}
	
	public static double GetShotAngle(Point ball, int ball_owner_team) {
		return GeometryUtil.Angle(ball, Pitch.kPost[1 - ball_owner_team][0], Pitch.kPost[1 - ball_owner_team][1]);
	}
	
	public static double GetShotDistance(Point ball, int ball_owner_team) {
		return GeometryUtil.Distance(ball, GetGoalPosition(ball_owner_team));
	}
	
	public static Point GetGoalPosition(int ball_owner_team) {
		return Pitch.kGoalCenterPoint[1 - ball_owner_team];
	}
	
	public static double GetShotVerticalAngle(double distance) {
		if (distance <= 0.0) return Math.PI;
		return Math.atan(Pitch.kGoalHeight / distance);
	}
	
	public static void Rotate(Point co) {
		co.x = Pitch.kWidth - co.x;
		co.y = Pitch.kLength - co.y;
	}
	
	public static void Mirror(Point co) {
		co.y = Pitch.kLength - co.y;
	}
	
	public static void RandomPointinRadius(Point center, double min_radius, double max_radius, Point output) {
		Vector offset = new Vector(RandomUtil.SampleFromUniformDistribution(0, Math.PI * 2));
		offset.Scale(RandomUtil.SampleFromUniformDistribution(min_radius, max_radius));
		output.Set(center, offset);
	}
	
	public static boolean InBoundary(Point p) {
		return GeometryUtil.PointInRectangle(Pitch.kPitch, p);
	}
	
	public static Game.Directive CheckBoundary(Point start, Point end, int last_touch_team) {
		if (InBoundary(end)) return Game.kPlayOnDirective;
		LineSegment goal_path = GeometryUtil.CreateLineSegment(start, end);
		for (int i = 0; i < 2; ++i) {
			Point intersect = GeometryUtil.Intersect(Pitch.kGoalLines[i], goal_path);
			if (intersect != null) {
				return new Game.Directive(i == last_touch_team ? Game.State.CORNER_KICK : Game.State.GOAL_KICK,
						(1 - last_touch_team) == 0, intersect.x < Pitch.kHalfWidth);
			}
			intersect = GeometryUtil.Intersect(Pitch.kTouchLines[i], goal_path);
			if (intersect != null) {
				return new Game.Directive(Game.State.THROW_IN, (1 - last_touch_team) == 0, intersect);
			}
		}
		System.err.println(start + "->" + end);
		System.err.println("should never happen");
		System.exit(0);
		return null;
	}
	
	public static PlayerState FindNearestPlayer(TeamInfo team, Point p) {
		PlayerState nearest_player = null;
		double nearest_distance = Double.MAX_VALUE;
		for (PlayerState player : team.players) {
			double distance = GeometryUtil.Distance(p, player.position);
			if (distance < nearest_distance) {
				nearest_distance = distance;
				nearest_player = player;
			}
		}
		return nearest_player;
	}
	
	public static double RelativeDepth(Point p, int team_index) {
		return (team_index == 0 ? p.y : Pitch.kLength - p.y) / Pitch.kLength;
	}
	
	public static boolean IsPenalty(Point p, int ball_owner_team) {
		return (ball_owner_team == 0 ? p.y : Pitch.kLength - p.y) > Pitch.kLength - Pitch.kBoxLength &&
				p.x > Pitch.kHalfWidth -  Pitch.kBoxWidth / 2 && p.x < Pitch.kHalfWidth + Pitch.kBoxWidth / 2;
	}
	
	public static void CapBoundary(Point p) {
		p.x = Math.max(kLineWith, p.x);
		p.y = Math.max(kLineWith, p.y);
		p.x = Math.min(Pitch.kWidth - kLineWith, p.x);
		p.y = Math.min(Pitch.kLength - kLineWith, p.y);
	}
	
	
	public static double kEpislon = 0.000001;
	public static double kLineWith = 0.5;
	private static double kDefendLine = Pitch.kHalfLength - 10;
	private static double kMidFieldLine = Pitch.kLength - 60;
	private static double kAttackLine = Pitch.kLength - 10;
	private static double kDefendLineInDefense = 10;
	private static double kMidFieldLineInDefense = 60;
	private static double kAttackLineInDefense = Pitch.kHalfLength + 10;
}
