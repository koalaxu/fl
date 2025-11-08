package fl.data;

public class League {
	public long league_id;
	public long country_id;
	public String name;
	public enum Type {
		DOMESTIC_LEAGUE,
		DOMESTIC_CUP,
		DOMESTIC_SUPER_CUP,
		CONTINENTAL_CUP,
		CONTINENTAL_SUPER_CUP,
	}
	public Type type;
	public int level;
	
	public static int kGroupNumber = 8;
	public static int kTeamPerGroup = 4;
	public static int kKnockoutOffset = kTeamPerGroup * kGroupNumber;
	public static int kPromotionQuota = 3;
}
