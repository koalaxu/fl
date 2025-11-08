package fl.engine.utils;

import fl.data.PlayerLeagueInfo;
import fl.engine.data.LeagueData;
import fl.engine.data.PlayerData;
import fl.engine.data.PlayerLeagueData;
import fl.utils.FieldAccessor;

public class DisciplineUtil {
	// It will also update the discipline data, so don't repeatedly call.
	public static boolean ShouldSuspend(LeagueData league, PlayerData player, boolean update) {
		PlayerLeagueData player_league_data = player.GetLeagueInfo(league);
		PlayerLeagueInfo info = player_league_data.GetInfo();
		if (info == null) return false;
		boolean suspended = FieldAccessor.kZeroBased.Get(info.suspended) > 0;
		if (update) {
			if (suspended) {
				info.suspended = FieldAccessor.kZeroBased.Update(info.suspended, -1);
				if (info.suspended == null) {
					player_league_data.DeleteInfo();
				} else {
					player_league_data.WriteInfo();
				}
			} else {
				player_league_data.DeleteInfo();
			}
		}
		return suspended;
	}
	
	public static int kYellowsToSuspend = 3;
}
