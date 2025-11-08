package fl.data;

public class Event implements Comparable<Event> {
	public int week_id;
	public boolean mid_week;
	
	public League.Type game_type;
	public Integer round;
	
	public enum EventType {
		NONE,
		MATCH,
		YOUTH_PROMOTION,
		SEASON_END,
		CONTRACT_RENEW,
		SQUAD_NUMBER_ASSIGNMENT,
		TRANSFER,
		MATCH_DRAW,
		CONTINENTAL_LEAGUE_KNOCKOUT_DRAW,
	}
	public EventType type;
	
	@Override
	public int compareTo(Event o) {
		if (week_id < o.week_id) return -1;
		if (week_id > o.week_id) return 1;
		if (mid_week == o.mid_week) return 0;
 		return mid_week ? 1 : -1;
	}
}
