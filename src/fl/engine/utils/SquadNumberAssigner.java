package fl.engine.utils;

import java.util.ArrayList;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import fl.data.PlayerInfo;
import fl.data.Position;
import fl.engine.data.ClubData;
import fl.engine.data.PlayerData;
import fl.utils.ScoredObject;

public class SquadNumberAssigner {
	static public void AssignSquadNumber(Collection<PlayerInfo> players) {
		HashMap<Integer, PlayerInfo> number_to_player =
				new HashMap<Integer, PlayerInfo>();
		for (PlayerInfo player : players) {
			if (player.club_info == null || player.club_info.squad_number == null) continue;
			if (player.club_info.squad_number < number_rule.length) {
				player.club_info.squad_number = null;
				continue;
			}
			number_to_player.put(player.club_info.squad_number, player);
		}
		HashSet<PlayerInfo> assigned = new HashSet<PlayerInfo>();
		for (int i = 1; i < number_rule.length; ++i) {
			Position[] preferred_positions = number_rule[i];
			for (Position pos : preferred_positions) {
				List<ScoredObject<PlayerInfo>> scored_players = new ArrayList<ScoredObject<PlayerInfo>>();
				for (PlayerInfo player : players) {
					if (assigned.contains(player)) continue;
					if (player.position == pos.name) {
						scored_players.add(new ScoredObject<PlayerInfo>(player,
								PositionAnalyzer.GetPositionConfidence(player, pos.type) + kSecondPositionDiscount));
					}
					if (player.secondary_position == pos.name) {
						scored_players.add(new ScoredObject<PlayerInfo>(player,
								PositionAnalyzer.GetPositionConfidence(player, pos.type)));
					}
				}
				scored_players.sort(ScoredObject.SortByScore());
				if (!scored_players.isEmpty()) {
					PlayerInfo candidate = scored_players.get(0).element;
					number_to_player.remove(candidate.club_info.squad_number);
					candidate.club_info.squad_number = Integer.valueOf(i);
					number_to_player.put(i, candidate);
					assigned.add(candidate);
					break;
				}
			}
		}
		int number = 12;
		for (PlayerInfo player : players) {
			if (player.club_info.squad_number == null) {
				for (; number <= kMaxSquadNumber; ++number) {
					if (number_to_player.containsKey(number)) continue;
					player.club_info.squad_number = Integer.valueOf(number++);
					break;
				}
			}
		}
	}
	
	static public void AssignOneNumber(PlayerData player) {
		SortedSet<Integer> numbers = new TreeSet<Integer>();
		ClubData club = player.GetClub();
		for (PlayerData teammate : club.GetPlayers()) {
			Integer number = teammate.GetInfo().club_info.squad_number;
			if (number == null) continue;
			numbers.add(number);
		}
		for (int i = 12;; ++i) {
			if (!numbers.contains(i)) {
				player.GetInfo().club_info.squad_number = Integer.valueOf(i);
				return;
			}
		}
	}
	
	static private Position[][] number_rule = {
			{},  // 0
			{ Position.GoalKeeper },
			{ Position.RightBack },
			{ Position.LeftBack },
			{ Position.CentreBack },
			{ Position.CentreBack, Position.Sweeper },
			{ Position.DefensiveMidFielder, Position.CentralMidFielder },
			{ Position.RightWinger, Position.RightMidFielder },
			{ Position.CentralMidFielder },
			{ Position.CentreForward, Position.SecondStriker },
			{ Position.AttackingMidFielder, Position.CentralMidFielder },
			{ Position.LeftWinger, Position.LeftMidFielder },
	};
	
	static public int kMaxSquadNumber = 99;
	static private double kSecondPositionDiscount = 0.05;
}
