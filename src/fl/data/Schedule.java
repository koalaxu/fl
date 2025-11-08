package fl.data;

public class Schedule {
	public class Match {
		public int host;
		public int away;
		public boolean neutral_site;
		public int winner_id;
	}
	public class Round {
		public Match[] matches;
	}
	public Round[] rounds;
}
