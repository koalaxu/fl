package fl.engine.data;

import fl.utils.Point;

public class Game {
	public enum ActionType {
		IDLE,
		WALK,
		RUN,
		DASH,
		PASS,
		DRIBBLE,
		SHOT,
		TACKLE,
		PASS_CUT,
		SAVE,
		HEADER,
	}
	
	
	public static class Action {
		public Action(ActionType type) {
			this.type = type;
		}
		public ActionType type;
		public int player_index;
		public boolean volley_ball;
		public Point target;
	}
	
	public static enum MatchPhase {
		FIRST_HALF,
		SECOND_HALF,
		EXTRA_TIME_FIRST_HALF,
		EXTRA_TIME_SECOND_HALF,
		PENALTY_KICK,
	}
	
	public enum State {
		NORMAL,
		KICK_OFF,
		THROW_IN,
		GOAL_KICK,
		CORNER_KICK,
		FREE_KICK,
		PENALTY_KICK,
		ABANDON,
	}
	
	public static class Directive {
		public Directive() {
			this.state = State.NORMAL;
		}
		private Directive(State state) {
			this.state = state;
		}	
		public Directive(State state, boolean owned_by_home) {
			this.state = state;
			this.owned_by_home = owned_by_home;
		}		
		public Directive(State state, boolean owned_by_home, boolean is_left) {
			this.state = state;
			this.owned_by_home = owned_by_home;
			this.is_left = is_left;
		}
		public Directive(State state, boolean owned_by_home, Point position) {
			this.state = state;
			this.owned_by_home = owned_by_home;
			this.position = position;
		}
		public State state;
		public boolean owned_by_home;
		public boolean is_left;
		public Point position;
	}
	
	public static Directive kPlayOnDirective = new Directive();
	public static Directive kMatchOverDirective = new Directive(State.ABANDON);
	
	public static double kShortPassDistance = 40;
	public static double kMediumPassDistance = 80;
	public static double kLongPassDistance = 150;
	
	public static MatchPhase[] kNormalGame = { MatchPhase.FIRST_HALF, MatchPhase.SECOND_HALF };
	public static MatchPhase[] kKnockOutGame = { MatchPhase.FIRST_HALF, MatchPhase.SECOND_HALF,
			MatchPhase.EXTRA_TIME_FIRST_HALF, MatchPhase.EXTRA_TIME_SECOND_HALF};
}
