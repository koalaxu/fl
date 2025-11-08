package fl.data;

import java.util.Map;
import java.util.TreeMap;

public class Global {
	public int year;
	public int week;
	public boolean mid_week;
	
	// per-country quota
	public int[] country_ranks = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 };
	
	// price model parameters
	public long base_price = 1500000L;
	public double[] position_bias = {
			0.6,
			0.7,
			0.9,
			0.8,
			0.7,
			1.0,
			1.2,
			0.9,
			0.8,
			1.2,
			1.5,
			1.5,
			1.4,
			1.4,
	};
	
	// play mode
	public long player_club = -1;
	public Map<Long, Long> min_transfer_fee_override = new TreeMap<Long, Long>();
	public long[] preferred_lineup = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
}
