package fl.engine.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Supplier;

import fl.data.Global;
import fl.data.Match;
import fl.data.Player;
import fl.data.Transfer;
import fl.data.utils.JsonUtil;
import fl.db.DataBase;
import fl.db.Key;

public class DataAccessor {
	private DataAccessor(String sub_dir) {
		sub_dir_ = sub_dir;
		db_ = new DataBase(sub_dir);
		global_ = JsonUtil.ParseOneObjectFromJson(sub_dir_ + "global.json", Global.class);
		db_.Load();
	}
	
	static public DataAccessor GetDataAccessor(DataBase db) {
		return accessors.get(db);
	}
	
	static public DataAccessor CreateDataAccessor(String sub_dir) {
		DataAccessor accessor = new DataAccessor(sub_dir);
		accessors.put(accessor.db_, accessor);
		return accessor;
	}
	
	public void Flush() {
		db_.Flush();
		JsonUtil.WriteOneObjectToJson(sub_dir_ + "global.json", global_);
	}
	
	public void Close() {
		Flush();
	}
	
	public Global GetGlobal() {
		return global_;
	}
	
	public static void SetProgress(float progress) {
		progress_ = (int) (progress * 100);
	}
	
	public static int GetProgress() {
		return progress_;
	}
	
	private class IdIterator<T> implements Iterator<T> {
		private IdIterator(Supplier<Long> funcGetSize, Function<Long, T> funcIdToData) {
			this.funcGetSize = funcGetSize;
			this.funcIdToData = funcIdToData;
		}
		@Override
		public boolean hasNext() {
			return index_ <= funcGetSize.get();
		}

		@Override
		public T next() {
			return funcIdToData.apply(index_++);
		}
		
		long index_ = 1;
		Supplier<Long> funcGetSize;
		Function<Long, T> funcIdToData;
	}
	
	private class KeyIterator<T> implements Iterator<T> {
		private KeyIterator(Iterator<Key> iterator, Function<Key, T> funcKeyToData) {
			this.iterator = iterator;
			this.funcKeyToData = funcKeyToData;
		}
		@Override
		public boolean hasNext() {
			return iterator.hasNext();
		}

		@Override
		public T next() {
			return funcKeyToData.apply(iterator.next());
		}
		
		Iterator<Key> iterator;
		Function<Key, T> funcKeyToData;
	}
	
	public class Collection<T> implements Iterable<T> {
		private Collection(Iterator<T> iterator) {
			this.iterator = iterator;
		}
		@Override
		public Iterator<T> iterator() {
			return iterator;
		}
		public boolean IsEmpty() {
			return !iterator.hasNext();
		}
		Iterator<T> iterator;
	}
	
	public CountryData GetCountry(long id) {
		return new CountryData(db_, id);
	}
	
	public LeagueData GetLeague(long id) {
		return new LeagueData(db_, id);
	}
	
	public ClubData GetClub(long id) {
		return new ClubData(db_, id);
	}
	
	public PlayerData GetPlayer(long id) {
		return new PlayerData(db_, id);
	}
	
	public MatchData GetMatch(long league_id, int round_id, int match_id, long year) {
		return new MatchData(db_, new Key(league_id, round_id, match_id, year));
	}
	
	public Collection<CountryData> GetAllCountries() {
		return new Collection<CountryData>(
				new IdIterator<CountryData>(() -> db_.country_table.GetSize(), this::GetCountry));
	}
	
	public Collection<LeagueData> GetAllLeauges() {
		return new Collection<LeagueData>(
				new IdIterator<LeagueData>(() -> db_.league_table.GetSize(), this::GetLeague));
	}
	
	public Collection<ClubData> GetAllClubs() {
		return new Collection<ClubData>(
				new IdIterator<ClubData>(() -> db_.club_table.GetSize(), this::GetClub));
	}
	
	public Collection<PlayerData> GetAllPlayers() {
		return new Collection<PlayerData>(
				new IdIterator<PlayerData>(() -> db_.player_table.GetSize(), this::GetPlayer));
	}
	
	public Collection<PlayerData> GetAllActivePlayers() {
		return new Collection<PlayerData>(
				new KeyIterator<PlayerData>(db_.player_info_table.GetKeyIterator(),
						key -> {  return GetPlayer(key.keys[0]);  }));
	}
	
	public long GetAllLeagueSize() {
		return db_.league_table.GetSize();
	}
	
	public long GetAllClubSize() {
		return db_.club_table.GetSize();
	}
	
	public PlayerData AddPlayer() {
		long id = db_.player_table.AppendRow(new Player()).keys[0];
		return GetPlayer(id);
	}
	
	public void CreateOneTransfer(Transfer transfer) {
		db_.transfer_table.UpdateRow(transfer);
	}
	
	public List<TransferData> GetAllTransfers(int year) {
		SortedSet<Key> transfer_row_keys = new TreeSet<Key>();
		db_.transfer_table.GetKeysByForeignKey("year", year, transfer_row_keys);
		List<TransferData> transfers = new ArrayList<TransferData>();
		for (Key key : transfer_row_keys) {
			transfers.add(new TransferData(db_, key));
		}
		return transfers;
	}
	
	public MatchData CreateOneMatch(Match match) {
		return new MatchData(db_, match);
	}
	
	protected Collection<MatchData> GetMatches(Iterator<Key> match_key_iterator) {
		return new Collection<MatchData>(
				new KeyIterator<MatchData>(match_key_iterator, key -> {  return new MatchData(db_, key); }));
	}
	
	public void ClearMatchCommentary() {
		db_.match_commentary_table.Clear();
	}
	
	public MarketData CreateNewMarketData() {
		market_data_ = new MarketData();
		return market_data_;
	}
	
	public MarketData GetMarketData() {
		return market_data_;
	}
	
	public ClubData GetControlledClub() {
		if (global_.player_club <= 0) return null;
		return GetClub(global_.player_club);
	}
	
	private DataBase db_;
	private Global global_;
	private String sub_dir_;
	
	// Transient data
	private MarketData market_data_ = new MarketData();
 	
	static private HashMap<DataBase, DataAccessor> accessors = new HashMap<DataBase, DataAccessor>();
	
	private static int progress_ = 0;
}
