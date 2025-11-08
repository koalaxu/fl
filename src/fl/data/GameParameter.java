package fl.data;

public class GameParameter {
	public double max_passcut_distance = 5; // Distance to the path
	public double max_shotblock_distance = 10; // Distance to the path
	public double max_volley_passcut_distance = 30;  // Distance to the start and end
	public double min_pass_offset = max_passcut_distance;
	public double max_pass_offset = max_volley_passcut_distance;
	public double min_challenge_offset = 10;
	public double max_challenge_offset = 20;
	public double max_distance_to_compete_ball = 20;
	public double max_distance_to_challenge = 6;
	public double shot_on_post_probability = 0.1;
	public double corner_kick_probability = 0.7;
	public double no_foul_injury_probability = 0.001;
	public double competition_injury_probability = 0.0025;
	public double tackle_injury_probability = 0.009;
	public double box_foul_factor = 0.2;
	public double competition_foul_probability = 0.1;
	public double tackle_foul_probability = 0.3;
	public double card_probablity = 0.25;
	public double red_card_probablity = 0.05;
	public double second_card_factor = 0.25;
	public double injury_card_factor = 2.0;
}
