package fl.engine.data;

import fl.data.PlayerLeagueInfo;
import fl.data.PlayerLeagueRecord;
import fl.db.DataBase;
import fl.db.Key;

public class PlayerLeagueData extends KeyedData {

	protected PlayerLeagueData(DataBase db, Key key) {
		super(db, key);
	}
	
	public PlayerLeagueInfo GetInfo() {
		if (info_ == null) {
			info_ = db_.player_league_info_table.FindRow(key_);
		}
		return info_;
	}
	
	public PlayerLeagueInfo GetOrCreateInfo() {
		GetInfo();
		if (info_ == null) {
			info_ = new PlayerLeagueInfo();
			db_.player_league_info_table.SetKey(info_, key_);
		}
		return info_;
	}
	
	public void WriteInfo() {
		if (info_ == null) return;
		db_.player_league_info_table.UpdateRow(info_);
	}
	
	public void DeleteInfo() {
		db_.player_league_info_table.DeleteRow(key_);
		info_ = null;
	}
	
	public PlayerData GetPlayer() {
		return GetAccessor().GetPlayer(key_.keys[0]);
	}
	
	public void AddLeagueRecord(PlayerLeagueRecord league_record) {
		league_record.player_id = key_.keys[0];
		league_record.league_id = key_.keys[1];
		league_record.year = GetAccessor().GetGlobal().year;
		db_.player_league_history_table.UpdateRow(league_record);
	}
	
	public PlayerLeagueRecord GetLeagueRecord(int year) {
		Key key = new Key(key_.keys[0], key_.keys[1], year);
		return db_.player_league_history_table.FindRow(key);
	}

	private PlayerLeagueInfo info_;
}
