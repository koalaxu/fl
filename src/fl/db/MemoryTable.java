package fl.db;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.function.Predicate;

import fl.data.TableConfig;
import fl.data.utils.FileUtil;
import fl.data.utils.JsonUtil;

public class MemoryTable<RowType> extends Table<RowType> {
	public MemoryTable(TableConfig config, Class<?> row_type_class,
			Map<String, Predicate<Long>> foreign_key_verifiers) {
		super(config, row_type_class, foreign_key_verifiers);
		in_memory_rows_ = new TreeMap<Key, RowType>();
	}
	
	@Override
	public RowType FindRow(Key key) {
		RowType row = in_memory_rows_.get(key);
		return JsonUtil.DeepCopy(row, row_type_class_);
	}
	
	@Override
	protected void UpdateRowInternal(Key key, RowType row) {
		in_memory_rows_.put(key, row);
		dirty_shards_.add(shard_.GetShardKey(key));
	}
	
	@Override
	protected RowType DeleteRowInternal(Key key) {
		RowType ret = in_memory_rows_.remove(key);
		if (ret != null) dirty_shards_.add(shard_.GetShardKey(key));
		return ret;
	}
	
	@Override
	protected void ClearInternal() {
		in_memory_rows_.clear();
		dirty_shards_.clear();
	}
	
	@Override
	public Iterator<Entry<Key, RowType>> GetIterator() {
		return in_memory_rows_.entrySet().iterator();
	}
	
	@Override
	protected long LoadInternal() {
		List<RowType> rows = new ArrayList<RowType>();
		SortedSet<String> files = FileUtil.ListFile(config_.dir, shard_.GetFilePattern());
		for (String filename : files) {
			io_.Load(config_.dir + filename, rows, row_type_class_, true);
		}
		for (RowType row : rows) {
			in_memory_rows_.put(GetKey(row), row);
		}
		return in_memory_rows_.size();
	}
	
	@Override
	public void Flush() {
		TreeMap<Key, RowType> map = new TreeMap<Key, RowType>(shard_);
		map.putAll(in_memory_rows_);
		for (Key shard_key : dirty_shards_) {
			io_.Save(config_.dir + shard_.GetFileNameFromShardKey(shard_key),
					map.subMap(shard_.GetKeyLowerBound(shard_key), true,
							shard_.GetKeyUpperBound(shard_key), true).values(), row_type_class_, true);
		}
	}
	
	private TreeSet<Key> dirty_shards_ = new TreeSet<Key>();
	public TreeMap<Key, RowType> in_memory_rows_;
}
