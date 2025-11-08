package fl.data;

import java.util.Map;
import java.util.TreeMap;

import fl.data.Formation.FormationName;
import fl.data.Position.PositionCategory;

public class InitializationSetup {
	public int start_year;
	public int division_per_country = 2;
	public int club_per_division = 18;
	public int player_per_club = 25;
	public int goalkeeper_per_club = 3;
	
	public class CountryParameters {
		public String name;
		public int fan_base;
		public double fan_attend_ratio;
		public double fan_spend_ratio;
		public int fan_population;
		public double price_index;
		public double mean_youth_level;  // Decides how many youth candidates to choose from
		public int mean_games;           // Decides the simulation to boost ability
	}
	public CountryParameters[] countries;
	
	// The fans of one club is initialized by power law distribution (1, k).
	// fan_base specifies the total fans in the country.
	public double domestic_rank_x_lowerbound;
	public double fan_base_powerlaw_k;
	public double rank_x_lowerbound;  // larger x to avoid monopoly
	public long global_fan_base;
	
	public double std_dev_youth_level_adjustment = 2.0;
	public int min_stadium_size = 10000;
	public int max_stadium_size = 80000;
	
	public double wage_income_ratio = 0.8;
	public double wage_ability_exponent = 5.0;
	public Map<PositionCategory, Double> position_wage_weights = new TreeMap<PositionCategory, Double>() {
		private static final long serialVersionUID = 1L; {
			put(PositionCategory.GOAL_KEEPER, 0.7);
			put(PositionCategory.DEFENDER, 0.8);
			put(PositionCategory.MIDFIELDER, 0.9);
			put(PositionCategory.FORWARD, 1.0);
	}};
	
	public Map<FormationName, Double> formation_popularity;
}
