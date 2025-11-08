package fl.engine.data;

import java.util.HashMap
;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import fl.data.Ability;
import fl.data.Formation;
import fl.data.Position;
import fl.engine.data.Game.MatchPhase;
import fl.engine.utils.AbilityCalculator;
import fl.engine.utils.FormationUtil;
import fl.utils.Point;

public class MatchInfo {
	public MatchInfo() {
		teams[0] = new TeamInfo(0);
		teams[1] = new TeamInfo(1);
	}
	public static class PlayerState {
		public PlayerState(int team_index, int player_index) {
			this.team_index = team_index;
			this.player_index = player_index;
		}
		public Position pos;
		public int team_index;
		public int player_index;
		public Point base_position;
		public Point position = new Point(-1, -1);
		public Ability ability;
		public PlayerData player;
		public double ability_multiplier = 1.0;
		public double stamina;
		public int disabled;
		public int injury;
		public double GetAbility(Ability.Type type) {
			return AbilityCalculator.GetAbility(this, type) * ability_multiplier;
		}
	}

	public static class TeamInfo {
		public TeamInfo(int team_index) {
			for (int i = 0; i < 11; ++i) players[i] = new PlayerState(team_index, i);
		}
		public Formation formation;
		public PlayerState[] players = new PlayerState[11];
		public Set<PlayerData> substitues;
		public FormationUtil.FormationSetting FormationSetting() {
			return FormationUtil.settings.get(formation);
		}
		public boolean formation_changed;
		public int subsitition_chances = 5;
		
		public int first_leg_goals = 0;
	}
	
	public static class Ball {
		public Point position = new Point(-1, -1);
		public boolean volley;
		public int owner_team = -1;
		public PlayerState ball_owner;
		public boolean competition;
	}
	
	public TeamInfo[] teams = new TeamInfo[2];
	public Map<PlayerData, Integer> player_stamina = new HashMap<PlayerData, Integer>();
	public Map<PlayerData, Integer> player_injury = new HashMap<PlayerData, Integer>();
	public Ball ball = new Ball();
	public PlayerState passer;
	public PlayerState assistant;
	
	public Game.State status;
	
	public MatchPhase phase = MatchPhase.FIRST_HALF;
	public int time = 0;  // in Seconds
	public int stopage_time = -1;
	
	public TeamInfo AttackTeam() {
		if (ball.owner_team == -1) return null;
		return teams[ball.owner_team];
	}
	
	public TeamInfo DefenderTeam() {
		if (ball.owner_team == -1) return null;
		return teams[1 - ball.owner_team];
	}
	
	public void DoForAllPlayers(Consumer<PlayerState> func) {
		for (int i = 0; i < 2; ++i)
			for (PlayerState player : teams[i].players) {
				func.accept(player);
			}
	}
}
