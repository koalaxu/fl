package fl.engine.data;

import fl.data.Transfer;
import fl.db.DataBase;
import fl.db.Key;

public class TransferData extends KeyedData{
	protected TransferData(DataBase db, Key key) {
		super(db, key);
		transfer_ = db_.transfer_table.FindRow(key);
	}

	public static Transfer MakeTransfer(
			PlayerData player, ClubData new_club, int current_year, long transfer_fee) {
		Transfer transfer = new Transfer();
		transfer.player_id = player.GetKey();
		transfer.from_club_id = player.GetInfo().club_id;
		transfer.to_club_id = new_club.GetKey();
		transfer.year = current_year;
		transfer.fee = transfer_fee;
		if (player.GetInfo().club_info != null) transfer.old_contract = player.GetInfo().club_info.contract;
		return transfer;
	}
	
	public PlayerData GetPlayer() {
		if (transfer_ == null) return null;
		return GetAccessor().GetPlayer(transfer_.player_id);
	}
	
	public boolean Valid() {
		return transfer_ != null;
	}
	
	public int GetYear() {
		return transfer_.year;
	}
	
	public ClubData GetFromClub() {
		return GetAccessor().GetClub(transfer_.from_club_id);
	}
	
	public ClubData GetToClub() {
		return GetAccessor().GetClub(transfer_.to_club_id);
	}
	
	public long GetFee() {
		return transfer_.fee;
	}
	
	public int GetContractLength() {
		return transfer_.old_contract;
	}

	private Transfer transfer_;
}
