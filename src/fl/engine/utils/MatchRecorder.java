package fl.engine.utils;

import java.util.Map;


import fl.data.MatchResult.Event;
import fl.data.MatchResult.EventType;
import fl.data.MatchStats;
import fl.data.MatchResult.TeamData;
import fl.engine.data.Game.ActionType;
import fl.engine.data.Game.Directive;
import fl.engine.data.MatchData;
import fl.engine.data.MatchInfo;
import fl.engine.data.MatchInfo.PlayerState;
import fl.engine.data.MatchInfo.TeamInfo;
import fl.engine.data.PlayerData;

public class MatchRecorder {
	public MatchRecorder(MatchInfo match_info, MatchData match_data) {
		this.match_info = match_info;
		this.match_data = match_data;
	}
	
	public void RecordLineup() {
		for (int i = 0; i < 2; ++i) RecordTeamLineup(match_info.teams[i], GetTeamData(i));
	}
	
	public void RecordAction(PlayerState player, ActionType action_type, boolean success) {
		MatchStats stats = GetPlayerStats(player);
		switch (action_type) {
		case DRIBBLE:
			if (success) stats.dribble_succeed++;
			stats.dribble++; 
			return;
		case PASS:
			if (success) stats.pass_succeed++;
			else stats.pass++;
			return;
		case SHOT:
			if (success) stats.shot_ontarget++;
			else stats.shot++;
			return;
		case HEADER:
			if (success) stats.header_succeed++;
			stats.header++; 
			return;
		case PASS_CUT:
			if (success) stats.intecept++;
			return;
		case TACKLE:
			if (success) stats.tackle++;
			return;
		case SAVE:
			if (success) stats.save++;
			return;
		default:
			break;
		}
	}

	public void RecordKeyPass(PlayerState player) {
		GetPlayerStats(player).key_pass++;
	}
	
	public void RecordClearance(PlayerState player) {
		GetPlayerStats(player).clearance++;
	}
	
	public void RecordGoal(boolean penalty) {
		GetPlayerStats(match_info.ball.ball_owner).goal++;
		GetPlayerStats(match_info.DefenderTeam().players[0]).goal_conceded++;
		PlayerState assistant = null;
		if (match_info.assistant != null && match_info.assistant != match_info.ball.ball_owner) {
			GetPlayerStats(match_info.assistant).assistance++;
			assistant = match_info.assistant;
		}
		GetTeamData(match_info.ball.owner_team).stats.goals++;
		GetTeamData(match_info.ball.owner_team).events.add(
				CreateEvent(penalty ? EventType.PENALTY_GOAL : EventType.GOAL,
					match_info.ball.ball_owner, assistant != null ? assistant.player : null));
	}
	
	public void RecordFoul(PlayerState defender) {
		GetPlayerStats(defender).foul++;
		GetPlayerStats(match_info.ball.ball_owner).fouled++;
	}
	
	public void RecordSetPiece(Directive directive) {
		TeamData team_data = directive.owned_by_home ? match_data.GetResult().home : match_data.GetResult().away;
		switch(directive.state) {
		case CORNER_KICK:
			team_data.stats.corner_kick++;
			return;
		case FREE_KICK:
			team_data.stats.free_kick++;
			return;
		case PENALTY_KICK:
			team_data.stats.penalty_kick++;
			return;
		default:
		}
	}
	
	public void RecordBook(PlayerState player, boolean red) {
		MatchStats stats = GetPlayerStats(player);
		if (red) stats.red = 1;
		else {
			stats.yellow += 1;
			if (stats.yellow == 2) {
				stats.red = 1;
				stats.yellow = 0;
			}
		}
		GetTeamData(player.team_index).events.add(CreateEvent(red ? EventType.RED : EventType.YELLOW, player));
	}
	
	public void ExchangeControl() {
		TeamData team_data = GetTeamData(match_info.ball.owner_team);
		if (team_data != null) team_data.stats.control_time += (match_info.time - control_start_time);
		control_start_time = match_info.time;
	}
	
	public void RecordSubstitution(PlayerState player, PlayerData substitute) {
		RecordPlayerOff(player);
		GetPlayerStats(player.team_index, substitute.GetKey()).start_time = GameTimeUtil.GetAbsoluteTime(match_info);
		GetTeamData(player.team_index).events.add(CreateEvent(EventType.SUBSTITUTE, player, substitute));
	}
	
	public void RecordPlayerOff(PlayerState player) {
		GetPlayerStats(player).end_time = GameTimeUtil.GetAbsoluteTime(match_info);
		if (player.injury > 0) match_info.player_injury.put(player.player, player.injury);
		if (player.stamina < 100) match_info.player_stamina.put(player.player, (int) player.stamina);
	}
	
	public MatchStats GetPlayerStats(PlayerState player) {
		return GetPlayerStats(player.team_index, player.player.GetKey());
	}
	
	public void AbandonGame(int team_index) {
		GetTeamData(team_index).stats.abandoned = true;
	}
	
	private TeamData GetTeamData(int team_index) {
		if (team_index < 0) return null;
		return team_index == 0 ? match_data.GetResult().home : match_data.GetResult().away;
	}
	
	private MatchStats GetPlayerStats(int team_index, long player_id) {
		Map<Long, MatchStats> player_stats = GetTeamData(team_index).player_stats;
		MatchStats stats = player_stats.get(player_id);
		if (stats == null) {
			stats = new MatchStats();
			player_stats.put(player_id, stats);
		}
		return stats;
	}

	private void RecordTeamLineup(TeamInfo team_info, TeamData team_data) {
		team_data.formation = team_info.formation.name;
		for (int i = 0; i < 11; ++i) {
			team_data.lineup[i] = team_info.players[i].player.GetKey();
		}
	}
	
	private Event CreateEvent(EventType type, PlayerState player) {
		Event event = new Event();
		event.time = GameTimeUtil.GetAbsoluteTime(match_info);
		event.type = type;
		event.player1 = player.player.GetKey();
		return event;
	}
	
	private Event CreateEvent(EventType type, PlayerState player, PlayerData player2) {
		Event event = CreateEvent(type, player);
		if (player2 != null) event.player2 = player2.GetKey();
		return event;
	}
	
	private MatchInfo match_info;
	private MatchData match_data;
	private long control_start_time = 0;
}
