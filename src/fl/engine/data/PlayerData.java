package fl.engine.data;

import java.util.ArrayList
;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import fl.data.Player;
import fl.data.PlayerHistory;
import fl.data.PlayerHistory.InjuryRecord;
import fl.data.PlayerHistory.PlayerRecord;
import fl.data.PlayerInfo;
import fl.data.Position;
import fl.db.DataBase;
import fl.db.Key;
import fl.engine.utils.PositionAnalyzer;

public class PlayerData extends IdKeyedData {

	protected PlayerData(DataBase db, long key) {
		super(db, key);
	}
	
	public Player GetData() {
		if (player_ == null) {
			player_ = db_.player_table.FindRow(new Key(key_));
		}
		return player_;
	}
	
	public PlayerInfo GetInfo() {
		if (player_info_ == null) {
			player_info_ = db_.player_info_table.FindRow(new Key(key_));
		}
		return player_info_;
	}
	
	public int GetAge() {
		return GetAccessor().GetGlobal().year - GetData().birth_year;
	}
	
	public Position GetPosition() {
		if (GetInfo() != null) return GetInfo().GetPosition();
		return Position.positions.get(GetData().position);
	}
	
	public ClubData GetClub() {
		if (GetInfo() == null) return null;
		long club_id = GetInfo().club_id;
		if (club_id == 0) return null;
		return GetAccessor().GetClub(club_id);
	}
	
	public PlayerLeagueData GetLeagueInfo(LeagueData league) {
		return new PlayerLeagueData(db_, new Key(key_, league.GetKey()));
	}
	
	public void SetData(Player player) {
		player_ = player;
		player_.player_id = key_;
	}
	
	public void SetInfo(PlayerInfo player_info) {
		player_info_ = player_info;
		player_info_.player_id = key_;
	}
	
	public void WriteData() {
		if (player_ != null) db_.player_table.UpdateRow(player_);
	}
	
	public void WriteInfo() {
		if (player_info_ != null) db_.player_info_table.UpdateRow(player_info_);
	}
	
	public void DeleteInfo() {
		if (GetInfo() == null) return;
		GetData().position = GetInfo().position;
		WriteData();
		db_.player_info_table.DeleteRow(new Key(key_));
		SortedSet<Key> player_league_keys = new TreeSet<Key>();
		db_.player_league_info_table.GetKeysByForeignKey("player_id", key_, player_league_keys);
		for (Key key : player_league_keys) {
			db_.player_league_info_table.DeleteRow(key);
		}
	}
	
	public void AddInjuryRecord(int length) {
		LoadPlayerHistory();
		if (player_history.injury_history == null) player_history.injury_history = new ArrayList<InjuryRecord>();
		player_history.injury_history .add(new InjuryRecord(
				GetAccessor().GetGlobal().year, GetAccessor().GetGlobal().week, length));
		db_.player_history_table.UpdateRow(player_history);
	}
	
	public List<InjuryRecord> GetInjuryRecord() {
		LoadPlayerHistory();
		return player_history.injury_history;
	}
	
	public void WriteAnnualRecord() {
		LoadPlayerHistory();
		if (player_history.annual_record == null) {
			player_history.annual_record = new TreeMap<Integer, PlayerRecord>();
		}
		PlayerRecord record = new PlayerRecord();
		record.club_id = GetClub().GetKey();
		record.squad_number = GetInfo().club_info.squad_number;
		record.wage = GetInfo().club_info.wage;
		record.score = PositionAnalyzer.GetOverallScore(player_info_);
		player_history.annual_record.put(GetAccessor().GetGlobal().year, record);
		db_.player_history_table.UpdateRow(player_history);
	}
	
	public SortedMap<Integer, PlayerRecord> GetAnnualRecords() {
		LoadPlayerHistory();
		return player_history.annual_record;
	}
	
	public PlayerRecord GetAnnualRecord(long year) {
		SortedMap<Integer, PlayerRecord> records = GetAnnualRecords();
		if (records == null) return null;
		return records.get((int)year);
	}
	
	public List<TransferData> GetTransfers() {
		SortedSet<Key> row_keys = new TreeSet<Key>();
		db_.transfer_table.GetKeysByForeignKey("player_id", key_, row_keys);
		List<TransferData> ret = new ArrayList<TransferData>();
		for (Key key : row_keys) {
			ret.add(new TransferData(db_, key));
		}
		return ret;
	}
	
	private void LoadPlayerHistory() {
		if (player_history != null) return;
		player_history = db_.player_history_table.FindRow(new Key(key_));
		if (player_history == null) {
			player_history = new PlayerHistory();
			player_history.player_id = key_;
		}
	}

	private Player player_;
	private PlayerInfo player_info_;
	private PlayerHistory player_history;
}
