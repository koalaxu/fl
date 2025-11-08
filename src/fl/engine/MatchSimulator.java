package fl.engine;

import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import fl.data.MatchResult;
import fl.data.MatchResult.TeamData;
import fl.data.MatchStats;
import fl.data.PlayerInfo;
import fl.data.PlayerLeagueInfo;
import fl.engine.data.ClubData;
import fl.engine.data.DataAccessor;
import fl.engine.data.LeagueData;
import fl.engine.data.MatchData;
import fl.engine.data.MatchInfo;
import fl.engine.data.MatchInfo.TeamInfo;
import fl.engine.data.PlayerData;
import fl.engine.data.PlayerLeagueData;
import fl.engine.utils.AbilityController;
import fl.engine.utils.DisciplineUtil;
import fl.engine.utils.FormationPicker;
import fl.engine.utils.FormationPicker.Lineup;
import fl.utils.FieldAccessor;
import fl.engine.utils.FormationUtil;
import fl.engine.utils.GameLogger;
import fl.engine.utils.LeagueUtil;
import fl.engine.utils.LeagueUtil.FirstLegGoals;
import fl.engine.utils.SquadNumberAssigner;
import fl.engine.utils.WageModel;
import fl.engine.utils.YouthUtil;

public class MatchSimulator extends BaseComponent {

	protected MatchSimulator(DataAccessor data_accessor, ComponentHub component_hub) {
		super(data_accessor, component_hub);
	}
	
	public void ProceedOneMatch(MatchData match, MatchResult first_leg_result, GameLogger logger) {
		LeagueData league = match.GetLeague();
		MatchInfo match_info = new MatchInfo();
		if (first_leg_result != null) {
			FirstLegGoals goals = LeagueUtil.GetFirstLegGoals(first_leg_result);
			match_info.teams[0].first_leg_goals = goals.away_goals;
			match_info.teams[1].first_leg_goals = goals.home_goals;
		}
		PrepareLineup(league, match.GetHomeClub(), match_info.teams[0]);
		PrepareLineup(league, match.GetAwayClub(), match_info.teams[1]);
		logger.SetMatchInfo(match_info);
		MatchEngine engine = new MatchEngine(match, match_info, logger);
		engine.Start();
		
		UpdatePlayerStatus(match_info);
		UpdateDiscipline(match.GetResult().home, league);
		UpdateDiscipline(match.GetResult().away, league);
		UpdateExp(match.GetResult().home, league);
		UpdateExp(match.GetResult().away, league);
	}
	
	public Set<PlayerData> GetAvailablePlayers(LeagueData league, ClubData club, boolean update) {
		Set<PlayerData> available_players = new TreeSet<PlayerData>();
		for (PlayerData player : club.GetPlayers()) {
			if (DisciplineUtil.ShouldSuspend(league, player, update)) continue;
			if (player.GetInfo().injury != null && player.GetInfo().injury > 0) continue;
			available_players.add(player);
		}
		return available_players;
	}
	
	private void PrepareLineup(LeagueData league, ClubData club, TeamInfo team_info) {
		Set<PlayerData> available_players = GetAvailablePlayers(league, club, true);
		while (available_players.size() < 11) {
			PlayerData player = GetComponentHub().youth_generator.PromoteOneYouthPlayer(
					YouthUtil.InitilaizeOnePlayer(club.GetInfo().youth_training_level), club, WageModel.kMinWage);
			SquadNumberAssigner.AssignOneNumber(player);
			player.WriteData();
			player.WriteInfo();
			available_players.add(player);
		}
		
		Lineup lineup = PickLineup(club, available_players);
		team_info.formation = lineup.formation;
		for (int i = 0 ; i < 11; ++i) {
			PlayerData player = lineup.players.get(i);
			team_info.players[i].pos = FormationUtil.GetFormationPositions(lineup.formation).get(i);
			team_info.players[i].player = player;
			available_players.remove(player);
		}
		team_info.substitues = available_players;
	}
	
	private Lineup PickLineup(ClubData club, Set<PlayerData> available_players) {
		Lineup lineup = null;
		if (club.equals(GetAccessor().GetControlledClub())) {
			lineup = FormationPicker.GetPrefferedLineup(club, available_players,
					GetAccessor().GetGlobal().preferred_lineup);
			if (lineup != null) return lineup;
		}
		return FormationPicker.Pick(club, available_players);
	}
	
	private void UpdatePlayerStatus(MatchInfo match_info) {
		for (Entry<PlayerData, Integer> entry : match_info.player_stamina.entrySet()) {
			GetComponentHub().condition_handler.UpdateStamina(entry.getKey(), entry.getValue().intValue());
		}
		for (Entry<PlayerData, Integer> entry : match_info.player_injury.entrySet()) {
			GetComponentHub().condition_handler.AddInjury(entry.getKey(), entry.getValue().intValue());
		}
	}
	
	private void UpdateDiscipline(TeamData team, LeagueData league) {
		for (Entry<Long, MatchStats> entry : team.player_stats.entrySet()) {
			int yellow = entry.getValue().yellow;
			int red = entry.getValue().red;
			if (yellow == 0 && red == 0) continue;
			if (yellow == 2) {
				red = 1;
				yellow = 0;
			}
			long player_id = entry.getKey();
			PlayerLeagueData player_league_data = GetAccessor().GetPlayer(player_id).GetLeagueInfo(league);
			PlayerLeagueInfo info = player_league_data.GetOrCreateInfo();
			if (yellow > 0) {
				info.yellow = FieldAccessor.kZeroBased.Update(info.yellow, 1);
				if ((info.yellow % DisciplineUtil.kYellowsToSuspend) == 0) {
					info.suspended = FieldAccessor.kZeroBased.Update(info.suspended, 1);
				}
			}
			if (red > 0) {
				info.suspended = FieldAccessor.kZeroBased.Update(info.suspended, 1);
			}
			player_league_data.WriteInfo();
		}
	}
	
	private void UpdateExp(TeamData team, LeagueData league) {
		for (Entry<Long, MatchStats> entry : team.player_stats.entrySet()) {
			long player_id = entry.getKey();
			PlayerData player = GetAccessor().GetPlayer(player_id);
			if (player == null) {
				System.err.println("Shouldn't have a null player: " + player_id);
			}
			if (AbilityController.GainExp(player, entry.getValue())) {
				PlayerInfo info = player.GetInfo();
				info.exp = FieldAccessor.kZeroBased.Update(info.exp,
						AbilityController.GetExp(player.GetAge(), league.GetData().level == 2));
				AbilityController.BoostAbilityByExperience(info);
				player.WriteInfo();
			}
		}
	}
	
}
