package fl.engine.utils;

import fl.data.MatchResult;

import fl.engine.data.Game.Action;
import fl.engine.data.Game.ActionType;
import fl.engine.data.Game.Directive;
import fl.engine.data.MatchInfo;
import fl.engine.data.MatchInfo.PlayerState;
import fl.engine.data.PlayerData;

public interface GameLogger {
	public void SetMatchInfo(MatchInfo match_info);
	public void LogDribble(PlayerState defender);
	public void LogRun();
	public void LogPass(Action action);
	public void LogShot();
	public void LogPassReseived(PlayerState receiver);
	public void LogDefendAction(PlayerState player, ActionType action_type, boolean get_ball);
	public void LogFoul(PlayerState defender);
	public void LogGetBall(PlayerState player);
	public void LogGoalSaving(boolean save_success, boolean caught);
	public void LogShootoutPenalty(PlayerState kicker, PlayerState gk, boolean on_target, boolean saved);
	public void LogShotOnPost();
	public void LogOutOfBoundary();
	public void LogBallReset(Directive directive);
	public void LogGoal();
	public void LogInjury();
	public void LogBooked(PlayerState player, boolean red, boolean second_yellow);
	public void LogSubstitution(PlayerState player, PlayerData substitute);
	public void LogScore(MatchResult result);
	public void LogAbandon(boolean is_home_team);
	public void LogEnd();
}
