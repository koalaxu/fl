package fl.engine.data;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import fl.data.Country;
import fl.data.League;
import fl.db.DataBase;
import fl.db.Key;

public class CountryData extends IdKeyedData {
	protected CountryData(DataBase db, long key) {
		super(db, key);
	}

	public Country GetData() {
		if (country_ == null) {
			country_ = db_.country_table.FindRow(new Key(key_));
		}
		return country_;
	}
	
	public List<ClubData> GetClubs() {
		SortedSet<Key> row_keys = new TreeSet<Key>();
		db_.club_table.GetKeysByForeignKey("country_id", key_, row_keys);
		List<ClubData> ret = new ArrayList<ClubData>();
		for (Key key : row_keys) {
			ret.add(GetAccessor().GetClub(key.keys[0]));
		}
		return ret;
	}
	
	public List<LeagueData> GetLeagues() {
		SortedSet<Key> row_keys = new TreeSet<Key>();
		db_.league_table.GetKeysByForeignKey("country_id", key_, row_keys);
		List<LeagueData> ret = new ArrayList<LeagueData>();
		for (Key key : row_keys) {
			ret.add(GetAccessor().GetLeague(key.keys[0]));
		}
		return ret;
	}
	
	public LeagueData GetLeague(League.Type type, int level) {
		List<LeagueData> leagues = GetLeagues();
		for (LeagueData league : leagues) {
			if (league.GetData().type == type && league.GetData().level == level) return league;
		}
		return null;
	}
	
	private Country country_;
}
