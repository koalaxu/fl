package fl.engine.utils;

import java.util.ArrayList;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fl.data.Formation;
import fl.data.PlayerInfo;
import fl.data.Position;
import fl.engine.data.ClubData;
import fl.engine.data.PlayerData;
import fl.utils.ScoredObject;

public class FormationPicker {
	private FormationPicker(Formation formation) {
		positions = FormationUtil.GetFormationPositions(formation);
		best_result = new Lineup(formation);
	}
	
	public static class Lineup {
		private Lineup(Formation formation) {
			this.formation = formation;
		}
		public double total_score = 0;
		public List<PlayerData> players;
		public Formation formation;
	}
	
	public static Lineup GetPrefferedLineup(
			ClubData club, Set<PlayerData> available_players, long[] preffered_players) {
		Lineup lineup = new Lineup(club.GetInfo().GetFavoriteFormation());
		lineup.players = new ArrayList<PlayerData>();
		Set<PlayerData> picked_players = new HashSet<PlayerData>();
		Map<Long, PlayerData> player_map = new HashMap<Long, PlayerData>();
		for (PlayerData player : available_players) {
			player_map.put(player.GetKey(), player);
		}
		for (int i = 0; i < 11; i++) {
			long player_id = preffered_players[i];
			PlayerData player = player_map.get(player_id);
			if (player == null || picked_players.contains(player)) return null;
			picked_players.add(player);
			lineup.players.add(player);
		}
		return lineup;
	}
	
	public static Lineup Pick(ClubData club, Collection<PlayerData> players) {
		Lineup lineup = Pick(club.GetInfo().GetFavoriteFormation(), players);
		if (lineup.players == null) {
			FillPlayers(lineup, players);
		}
		return lineup;
	}
	
	public static Lineup Pick(Formation favorite_formation, Collection<PlayerData> players) {
		Lineup lineup = new Lineup(favorite_formation);
		for (Formation formation : Formation.formations.values()) {
			Lineup tmp_lineup = PickOneFormation(formation, players);
			if (tmp_lineup == null) continue;
			if (formation == favorite_formation) tmp_lineup.total_score *= kFavoriteFormationMultiplier;
			if (lineup.total_score < tmp_lineup.total_score) {
				lineup = tmp_lineup;
			}
		}
		return lineup;
	}
	
	public static Lineup PickOneFormation(Formation formation, Collection<PlayerData> players) {
		return new FormationPicker(formation).PickUp(players);
	}
	
	public Lineup PickUp(Collection<PlayerData> players) {
		for (Position position : positions) {
			List<ScoredObject<PlayerData>> candidates = new ArrayList<ScoredObject<PlayerData>>();
			for (PlayerData player : players) {
				PlayerInfo player_info = player.GetInfo();
				if (player_info.GetPosition() == position || player_info.GetSecondaryPosition() == position) {
					double score = PositionAnalyzer.GetOverallScore(player_info, position)
							* AbilityCalculator.ConditionMultiplier(player_info.condition, player_info.stamina);
					candidates.add(new ScoredObject<PlayerData>(player, score));
				}
			}
			if (candidates.isEmpty()) return null;
			candidates.sort(ScoredObject.SortByScore());
			if (candidates.size() > kMaxCandidatePerPosition) {
				candidates = candidates.subList(0, kMaxCandidatePerPosition - 1);
			}
			pos_candidates.add(candidates);
		}
		cumulated_score.add(0.0);
		PickOnePosition(0);
		if (best_result.players == null) return null;
		return best_result;
	}
	
	private void PickOnePosition(int index) {
		for (ScoredObject<PlayerData> scored_candidate : pos_candidates.get(index)) {
			PlayerData candidate = scored_candidate.element;
			if (lineups.contains(candidate)) continue;
			lineups.add(candidate);
			cumulated_score.add(cumulated_score.getLast() + scored_candidate.score); 
			if (index < 10) {
				PickOnePosition(index + 1);
			} else {
				if (cumulated_score.getLast() > best_result.total_score) {
					best_result.total_score = cumulated_score.getLast();
					best_result.players = new ArrayList<PlayerData>(lineups);
				}
			}
			lineups.removeLast();
			cumulated_score.removeLast();
		}
	}
	
	private static void FillPlayers(Lineup lineup, Collection<PlayerData> players) {
		lineup.players = new ArrayList<PlayerData>();
		List<Position> positions = FormationUtil.GetFormationPositions(lineup.formation);
		Set<PlayerData> selected_players = new HashSet<PlayerData>();
		for (Position position : positions) {
			PlayerData candidate = ScoredObject.GetMax(players,
					player -> AbilityCalculator.PlayerScore(player.GetInfo(), position),
					player -> selected_players.contains(player)).element;
			selected_players.add(candidate);
			lineup.players.add(candidate);
		}
	}
	
	private List<Position> positions;
	private List<List<ScoredObject<PlayerData>>> pos_candidates =
			new ArrayList<List<ScoredObject<PlayerData>>>();
	private LinkedList<PlayerData> lineups = new LinkedList<PlayerData>();
	private LinkedList<Double> cumulated_score = new LinkedList<Double>();
	
	private Lineup best_result;
	
	private static int kMaxCandidatePerPosition = 5;
	private static double kFavoriteFormationMultiplier = 1.05;
}
