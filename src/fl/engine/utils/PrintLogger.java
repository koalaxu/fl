package fl.engine.utils;

import java.io.PrintStream;

import fl.data.MatchResult;
import fl.data.PlayerInfo;
import fl.engine.data.Game.Action;
import fl.engine.data.Game.ActionType;
import fl.engine.data.Game.Directive;
import fl.engine.data.Game;
import fl.engine.data.MatchInfo;
import fl.engine.data.MatchInfo.PlayerState;
import fl.engine.data.PlayerData;

public class PrintLogger implements GameLogger {
	public void SetMatchInfo(MatchInfo match) {
		this.match = match;
	}
	
	@Override
	public void LogDribble(PlayerState defender) {
		Print(PrintPlayer(match.ball.ball_owner) + " dribbles against " + PrintPlayer(defender));
	}
	
	@Override
	public void LogRun() {
		if (runner == match.ball.ball_owner) return;
		Print(PrintPlayer(match.ball.ball_owner) + " runs..");
		runner = match.ball.ball_owner;
	}
	
	@Override
	public void LogPass(Action action) {
		String str = PrintPlayer(match.ball.ball_owner) + (action.volley_ball ? " volley" : "") + " passes to ";
		if (show_position) str = str + match.AttackTeam().players[action.player_index].position + " ...";
		else str = str + PrintPlayer(match.AttackTeam().players[action.player_index]) + " ...";
		Print(str);
				
	}
	
	@Override
	public void LogShot() {
		Print(PrintPlayer(match.ball.ball_owner) + " shots!");
	}
	
	@Override
	public void LogPassReseived(PlayerState receiver) {
		Print(PrintPlayer(receiver) + " receives the ball.");
	}
	
	@Override
	public void LogDefendAction(PlayerState player, ActionType action_type, boolean get_ball) {
		String str = PrintPlayer(player) + " ";
		if (!get_ball) {
			str = str + "clears the ball.";
		} else if (action_type == ActionType.PASS_CUT) {
			str = str + "cuts the ball.";
		} else if (action_type == ActionType.TACKLE) {
			str = str + "tackles the ball.";
		}
		Print(str);
	}
	
	@Override
	public void LogFoul(PlayerState defender) {
		Print(PrintPlayer(match.ball.ball_owner) + " falls down. " + PrintPlayer(defender) + " fouls!");
	}
	
	@Override
	public void LogGetBall(PlayerState player) {
		Print(PrintPlayer(player) + " gets the ball.");
	}
	
	@Override
	public void LogGoalSaving(boolean save_success, boolean caught) {
		String str = PrintPlayer(match.DefenderTeam().players[0]) + " ";
		if (!save_success) {
			str = str + "fails to touch the ball.";
		} else if (caught) {
			str = str + "catches the ball.";
		} else {
			str = str + "punches the ball.";
		}
		Print(str);
	}
	
	@Override
	public void LogShootoutPenalty(PlayerState kicker, PlayerState gk, boolean on_target, boolean saved) {
		Print(PrintPlayer(kicker) + " shots!");
		if (!on_target) {
			Print("The ball is off target!!!");
		} else if (saved) {
			Print(PrintPlayer(gk) + " saves!!!");
		} else {
			Print("Goal!!!");
		}
	}
	
	@Override
	public void LogShotOnPost() {
		Print("The ball hits on the post!!!");
	}
	
	@Override
	public void LogOutOfBoundary() {
		Print("The ball is out of pitch.");
	}
	
	@Override
	public void LogBallReset(Directive directive) {
		Print(PrintTeam(directive.owned_by_home) + "'s " + directive.state);
	}
	
	@Override
	public void LogGoal() {
		Print("Goal!!!");
	}
	
	@Override
	public void LogInjury() {
		Print(PrintPlayer(match.ball.ball_owner) + " injured. ");
	}
	
	@Override
	public void LogBooked(PlayerState player, boolean red, boolean second_yellow) {
		Print(PrintPlayer(player) + " is brandished a " + (red ? "red" : "yellow") + " card!");
		if (second_yellow) {
			Print("It's his second yellow card. He's sent off");
		} else if (red) {
			Print("He's sent off");
		}
	}
	
	@Override
	public void LogSubstitution(PlayerState player, PlayerData substitute) {
		Print(PrintPlayer(player) + " off; " + PrintPlayer(substitute) + " on.");
	}
	
	@Override
	public void LogScore(MatchResult result) {
		String str = "Current Score: " + result.home.stats.goals + " : " + result.away.stats.goals;
		if (match.phase == Game.MatchPhase.PENALTY_KICK) {
			str += " (" + result.home.stats.shootout_goals + " : " + result.away.stats.shootout_goals + ")";
		}
		Print(str);
	}
	
	@Override
	public void LogAbandon(boolean is_home_team) {
		Print(PrintTeam(is_home_team) + " has fewer than 7 players. Game abandoned!");
	}
	
	@Override
	public void LogEnd() {
		Print("End of " + match.phase);
	}
	
	private void Print(String output) {
		out.print(GameTimeUtil.TimeString(match) + " ");
		out.println(output);
		runner = null;
	}
	
	private String PrintPlayer(PlayerState player) {
		PlayerInfo player_info = player.player.GetInfo();
		String str = (player.team_index == 0 ? "[H]" : "[A]")
				+ player.pos.abbreviate + "-" + player_info.club_info.squad_number + " "
				+ player.player.GetData().name;
		if (show_stamina) str = str + "(" + String.format("%.1f%%", player.stamina) + ")";
		if (show_position) str = str + player.position;
		return str;
	}
	
	private String PrintPlayer(PlayerData player) {
		PlayerInfo player_info = player.GetInfo();
		return player_info.club_info.squad_number + " " + player.GetData().name;
	}
	
	private String PrintTeam(boolean is_home_team) {
		return is_home_team ? "Home team" : "Away team";
	}

	protected PrintStream out = System.out;
	protected boolean show_position = true;
	protected boolean show_stamina = true;
	
	private MatchInfo match;
	private PlayerState runner;
}
