package fl.db;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.function.Predicate;

import fl.data.TableConfig;
import fl.data.utils.FileUtil;
import fl.data.utils.JsonUtil;

public class OnDiskTable<RowType> extends Table<RowType> {

	protected OnDiskTable(TableConfig config, Class<?> row_type_class,
			Map<String, Predicate<Long>> foreign_key_verifiers) {
		super(config, row_type_class, foreign_key_verifiers);
		in_memory_rows_ = new TreeMap<Key, RowType>();
	}
	
	private class RowIterator implements Iterator<Entry<Key, RowType>> {
		
		public RowIterator() {
			files_ = FileUtil.ListFile(config_.dir, shard_.GetFilePattern());
			file_iter_ = files_.iterator();
			has_next_ = ProceedToNext();
		}
		
		@Override
		public boolean hasNext() {
			return has_next_;
		}

		@Override
		public Entry<Key, RowType> next() {
			RowType row = row_iter_.next();
			has_next_ = ProceedToNext();
			return new AbstractMap.SimpleEntry<Key, RowType>(GetKey(row), row);
		}
		
		private boolean ProceedToNext() {
			if (row_iter_ == null || !row_iter_.hasNext()) {
				if (!file_iter_.hasNext()) return false;
				rows_ = LoadShard(file_iter_.next());
				row_iter_ = rows_.iterator();
				if (!row_iter_.hasNext()) return false;
			}
			return true;
		}
		
		private List<RowType> LoadShard(String filename) {
			List<RowType> rows = new ArrayList<RowType>();
			io_.Load(config_.dir + filename, rows, row_type_class_, true);
			return rows;
		}
		
		SortedSet<String> files_ = null;
		Iterator<String> file_iter_ = null;
		List<RowType> rows_ = null;
		Iterator<RowType> row_iter_ = null;
		boolean has_next_ = false;
	}

	@Override
	public RowType FindRow(Key key) {
		MaybeSwitchShard(key);
		RowType row = in_memory_rows_.get(key);
		if (row == null) return null;
		return JsonUtil.DeepCopy(row, row_type_class_);
	}

	@Override
	protected void UpdateRowInternal(Key key, RowType row) {
		MaybeSwitchShard(key);
		in_memory_rows_.put(key, row);
		cur_shard_dirty_ = true;
	}

	@Override
	protected RowType DeleteRowInternal(Key key) {
		MaybeSwitchShard(key);
		RowType ret = in_memory_rows_.remove(key);
		cur_shard_dirty_ = (ret != null);
		return ret;
	}
	
	@Override
	protected void ClearInternal() {
		cur_shard_key_ = null;
		cur_shard_dirty_ = false;
		in_memory_rows_.clear();
	}

	@Override
	public Iterator<Entry<Key, RowType>> GetIterator() {
		Flush();
		return new RowIterator();
	}

	@Override
	protected long LoadInternal() {
		if (!config_.key_owner) return 0;
		SortedSet<String> files = FileUtil.ListFile(config_.dir, shard_.GetFilePattern());
		if (files.isEmpty()) return 0;
		cur_shard_key_ = shard_.GetShardKeyFromFileName(files.last());
		LoadShard(files.last());
		if (in_memory_rows_.isEmpty()) return 0;
		return in_memory_rows_.lastKey().keys[0];
	}

	@Override
	public void Flush() {
		if (cur_shard_dirty_ && cur_shard_key_ != null) {
			io_.Save(config_.dir + shard_.GetFileNameFromShardKey(cur_shard_key_),
					in_memory_rows_.values(), row_type_class_, true);
		}
		cur_shard_dirty_ = false;
	}
	
	private void LoadShard(String filename) {
		List<RowType> rows = new ArrayList<RowType>();
		io_.Load(config_.dir + filename, rows, row_type_class_, true);
		in_memory_rows_.clear();
		for (RowType row : rows) {
			in_memory_rows_.put(GetKey(row), row);
		}
	}
	
	private boolean MaybeSwitchShard(Key key) {
		Key shard_key = shard_.GetShardKey(key);
		if (cur_shard_key_ != null && cur_shard_key_.equals(shard_key)) return false;
		Flush();
		cur_shard_key_ = shard_key;
		LoadShard(shard_.GetFileNameFromShardKey(shard_key));
		return true;
	}

	public Key cur_shard_key_;
	public boolean cur_shard_dirty_ = false;
	public TreeMap<Key, RowType> in_memory_rows_;
}
