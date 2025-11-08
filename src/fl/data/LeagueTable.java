package fl.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class LeagueTable implements Serializable {
	private static final long serialVersionUID = 1L;

	public static class ClubStats implements Serializable  {
		private static final long serialVersionUID = 1L;
		public long club_id;
		public int win;
		public int draw;
		public int lose;
		public int goal;
		public int goal_conceded;
	}
	
	public List<ClubStats> clubs = new ArrayList<ClubStats>();
}
