package fl.engine;

import java.util.ArrayList;
import java.util.List;
import fl.data.PlayerInfo;
import fl.engine.data.ClubData;
import fl.engine.data.DataAccessor;
import fl.engine.data.PlayerData;
import fl.engine.utils.SquadNumberAssigner;

public class SquadProcessor extends BaseComponent {

	protected SquadProcessor(DataAccessor data_accessor, ComponentHub component_hub) {
		super(data_accessor, component_hub);
	}

	public void Process() {
		int processed = 0;
		for (ClubData club : GetAccessor().GetAllClubs()) {
			List<PlayerData> players = new ArrayList<PlayerData>();
			List<PlayerInfo> player_info = new ArrayList<PlayerInfo>();
			for (PlayerData player : club.GetPlayers()) {
				if (player.GetInfo().club_info == null || player.GetInfo().club_info.contract <= 0) {
					player.DeleteInfo();
					continue;
				}
				players.add(player);
				player_info.add(player.GetInfo());
			}
			SquadNumberAssigner.AssignSquadNumber(player_info);
			// Update
			for (int i = 0; i < players.size(); ++i) {
				players.get(i).SetInfo(player_info.get(i));
				players.get(i).WriteInfo();
			}
			DataAccessor.SetProgress((float)++processed / GetAccessor().GetAllClubSize());
		}
	}
}
