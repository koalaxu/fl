package fl.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import fl.data.Formation.FormationName;

public class MatchResult implements Serializable {
	private static final long serialVersionUID = 1L;
	public static class TeamStats implements Serializable {
		private static final long serialVersionUID = 1L;
		public long control_time;
		public int corner_kick;
		public int free_kick;
		public int penalty_kick;
		public int goals;
		public int shootout_goals;
		public boolean abandoned;
	}
	public enum EventType {
		GOAL,
		PENALTY_GOAL,
		SUBSTITUTE,
		YELLOW,
		RED,
	}
	public static class Event implements Serializable {
		private static final long serialVersionUID = 1L;
		public int time;
		public EventType type;
		public long player1;
		public long player2;
	}
	public static class TeamData implements Serializable {
		private static final long serialVersionUID = 1L;
		public FormationName formation;
		public long[] lineup = new long[11];
		public Map<Long, MatchStats> player_stats = new TreeMap<Long, MatchStats>();
		public TeamStats stats = new TeamStats();
		public List<Event> events = new ArrayList<Event>();
	}
	public TeamData home = new TeamData();
	public TeamData away = new TeamData();
	public boolean extra_time = false;
}
