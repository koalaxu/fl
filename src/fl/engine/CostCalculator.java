package fl.engine;

import fl.engine.data.ClubData;
import fl.engine.data.DataAccessor;
import fl.engine.data.PlayerData;

public class CostCalculator extends BaseComponent {

	protected CostCalculator(DataAccessor data_accessor, ComponentHub component_hub) {
		super(data_accessor, component_hub);
	}
	
	public long GetClubCost(ClubData club) {
		long cost = 0L;
		for (PlayerData player : club.GetPlayers()) {
			if (player.GetInfo().club_info == null || player.GetInfo().club_info.contract <= 0) continue;
			cost += player.GetInfo().club_info.wage;
		}
		return cost;
	}

}
