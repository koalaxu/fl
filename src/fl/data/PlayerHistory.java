package fl.data;

import java.util.List;
import java.util.SortedMap;

public class PlayerHistory {
	public long player_id;
	public static class InjuryRecord {
		public InjuryRecord(long year, int week, int length) {
			this.year = year;
			this.week = week;
			this.length = length;
		}
		public long year;
		public int week;
		public int length;
	}
	public List<InjuryRecord> injury_history;
	public static class PlayerRecord {
		public Integer squad_number;
		public int wage;
		public long club_id;
		public double score;
	}
	public SortedMap<Integer, PlayerRecord> annual_record;
}
