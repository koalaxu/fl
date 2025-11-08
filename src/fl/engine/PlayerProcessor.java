package fl.engine;

import fl.data.PlayerInfo;
import fl.data.PlayerInfo.PlayerClubInfo;
import fl.engine.data.DataAccessor;
import fl.engine.data.PlayerData;
import fl.engine.utils.AbilityController;

public class PlayerProcessor extends BaseComponent {

	protected PlayerProcessor(DataAccessor data_accessor, ComponentHub component_hub) {
		super(data_accessor, component_hub);
	}
	
	public void PassOneYear() {
		for (PlayerData player : GetAccessor().GetAllActivePlayers()) {
			PlayerInfo info = player.GetInfo();
			PlayerClubInfo club_info = info.club_info;
			if (club_info != null) club_info.contract--;
			AbilityController.UpdateAnnualAbility(info, player.GetAge());
			player.WriteInfo();
		}
	}
	
	public void RecordPlayerHistory() {
		for (PlayerData player : GetAccessor().GetAllActivePlayers()) {
			if (player.GetInfo() != null && player.GetInfo().club_info != null) {
				player.WriteAnnualRecord();
			}
		}
	}

}
