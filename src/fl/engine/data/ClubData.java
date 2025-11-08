package fl.engine.data;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import fl.data.Club;
import fl.data.ClubInfo;
import fl.data.ClubRecord;
import fl.data.League.Type;
import fl.db.DataBase;
import fl.db.Key;
import fl.engine.data.DataAccessor.Collection;

public class ClubData extends IdKeyedData {

	protected ClubData(DataBase db, long key) {
		super(db, key);
	}
	
	public Club GetData() {
		if (club_ == null) {
			club_ = db_.club_table.FindRow(new Key(key_));
		}
		return club_;
	}
	
	public ClubInfo GetInfo() {
		if (club_info_ == null) {
			club_info_ = db_.club_info_table.FindRow(new Key(key_));
		}
		return club_info_;
	}
	
	public void WriteData() {
		if (club_ != null) db_.club_table.UpdateRow(club_);
	}
	
	public void WriteInfo() {
		if (club_info_ != null) db_.club_info_table.UpdateRow(club_info_);
	}

	public CountryData GetCountry() {
		return GetAccessor().GetCountry(GetData().country_id);
	}
		
	public List<LeagueData> GetClubParticipatingLeagues() {
		List<LeagueData> ret = new ArrayList<LeagueData>();
		for (LeagueData league : GetAccessor().GetAllLeauges()) {
			if (league.HasClub(this)) {
				ret.add(league);
			}
		}
		return ret;
	}
	
	public List<PlayerData> GetPlayers() {
		List<PlayerData> ret = new ArrayList<PlayerData>();
		SortedSet<Key> row_keys = new TreeSet<Key>();
		db_.player_info_table.GetKeysByForeignKey("club_id", key_, row_keys);
		for (Key row_key : row_keys) {
			ret.add(GetAccessor().GetPlayer(row_key.keys[0]));
		}
		return ret;
	}
	
	public Collection<MatchData> GetMatches(int year) {
		SortedSet<Key> home_match_keys = new TreeSet<Key>();
		SortedSet<Key> away_match_keys = new TreeSet<Key>();
		db_.match_table.GetKeysByForeignKey("year", year, home_match_keys);
		away_match_keys.addAll(home_match_keys);
		db_.match_table.FilterKeysByForeignKey("home_club", key_, home_match_keys);
		db_.match_table.FilterKeysByForeignKey("away_club", key_, away_match_keys);
		home_match_keys.addAll(away_match_keys);
		return GetAccessor().GetMatches(home_match_keys.iterator());
	}
	
	public void AddRecord(ClubRecord record) {
		record.balance = GetInfo().fund;
		record.club_id = key_;
		record.year = GetAccessor().GetGlobal().year;
		db_.club_history_table.UpdateRow(record);
	}
	
	public SortedMap<Integer, ClubRecord> GetRecords() {
		SortedMap<Integer, ClubRecord> records = new TreeMap<Integer, ClubRecord>();
		SortedSet<Key> row_keys = new TreeSet<Key>();
		db_.club_history_table.GetKeysByForeignKey("club_id", key_, row_keys);
		for (Key row_key : row_keys) {
			ClubRecord record = db_.club_history_table.FindRow(row_key);
			records.put((int)record.year, record);
		}
		return records;
	}
	
	public void GetTransferIn(List<TransferData> transfers) {
		SortedSet<Key> row_keys = new TreeSet<Key>();
		db_.transfer_table.GetKeysByForeignKey("to_club_id", key_, row_keys);
		for (Key row_key : row_keys) {
			transfers.add(new TransferData(db_, row_key));
		}
	}
	
	public void GetTransferOut(List<TransferData> transfers) {
		SortedSet<Key> row_keys = new TreeSet<Key>();
		db_.transfer_table.GetKeysByForeignKey("from_club_id", key_, row_keys);
		for (Key row_key : row_keys) {
			transfers.add(new TransferData(db_, row_key));
		}
	}
	
	public LeagueData FindDomesticLeague() {
		for (LeagueData league : GetClubParticipatingLeagues()) {
			if (league.GetData().type == Type.DOMESTIC_LEAGUE) return league;
		}
		return null;
	}
	
	private Club club_;
	private ClubInfo club_info_;
}
