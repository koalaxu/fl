package fl.engine;

import java.util.List;
import java.util.Set;

import fl.data.Global;
import fl.engine.data.ClubData;
import fl.engine.data.DataAccessor;
import fl.engine.data.LeagueData;
import fl.engine.data.PlayerData;

public class ControllerModule {
	public ControllerModule(DataAccessor data_accessor, ComponentHub component_hub) {
		data_ = data_accessor;
		component_hub_ = component_hub;
	}
	
	public void OverrideMinTransferFee(long player_id, long fee) {
		Global global = data_.GetGlobal();
		global.min_transfer_fee_override.put(player_id, fee);		
		component_hub_.transfer.OverrideMinTransferFee(data_.GetPlayer(player_id), fee);
	}
	
	public enum BidStatus {
		ACCEPTED,
		REJECTED_TRANSFER_FEE,
		REJECTED_WAGE,
		NO_SUFFICIENT_FEE,
	}
	
	public BidStatus Bid(long player_id, long transfer_fee, int wage) {
		return component_hub_.transfer.Bid(
				data_.GetMarketData().FindSupply(data_.GetPlayer(player_id)), data_.GetControlledClub(),
				transfer_fee, wage);
	}
	
	public enum RenewalStatus {
		ACCEPTED,
		REJECTED_LENGTH,
		REJECTED_WAGE,
	}
	
	public RenewalStatus RenewContract(long player_id, int wage, int contract_length) {
		return component_hub_.contract_processor.RenewContract(
				data_.GetPlayer(player_id), wage, contract_length);
	}
	
	public List<PlayerData> GetUnavailablePlayers(ClubData club, LeagueData league) {
		Set<PlayerData> available = component_hub_.match_simulator.GetAvailablePlayers(league, club, false);
		List<PlayerData> players = club.GetPlayers();
		players.removeIf(player -> { return available.contains(player); });
		return players;
	}
	
	private DataAccessor data_;
	private ComponentHub component_hub_;
}
