package fl.db;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Predicate;

import fl.data.TableConfig;
import fl.data.TableConfig.KeyType;
import fl.data.utils.FileUtil;


public abstract class Table<RowType extends Object> {
	protected Table(TableConfig config, Class<?> row_type_class,
			Map<String, Predicate<Long>> foreign_key_verifiers) {
		this.config_ = config;
		this.row_type_class_ = row_type_class;
		this.foreign_key_verifiers_ = foreign_key_verifiers;
		this.shard_ = config.sharding_method == TableConfig.ShardingMethod.SINGLE_FILE ?
				new SinlgeShard(config) : new IdPartitionShard(config);
		if (config.file_format == TableConfig.FileFormat.JSON) {
			this.io_ = new JsonIO<RowType>();
		} else if (config.file_format == TableConfig.FileFormat.BINARY) {
			this.io_ = new BinaryIO<RowType>();
		} else {
			System.err.println("Unsupported file format: " + config.file_format);
			System.exit(0);
		}
	}
	
	static public <RowType> Table<RowType> BuildTable(
			TableConfig config, Class<?> row_type_class,
			Map<String, Predicate<Long>> foreign_key_verifiers) {
		if (config.loading_method == TableConfig.LoadingMethod.MEMORY) {
			return new MemoryTable<RowType>(config, row_type_class, foreign_key_verifiers);
		} else if  (config.loading_method == TableConfig.LoadingMethod.DISK) {
			return new OnDiskTable<RowType>(config, row_type_class, foreign_key_verifiers);
		}
		System.err.println("Unsupported loading method: " + config.loading_method);
		System.exit(0);
		return null;
	}

	public Key AppendRow(RowType row) {
		if (!config_.key_owner) {
			System.err.println("The key is not incremtantal ids.");
			System.exit(0);
			return null;
		}
		VerifyValue(row);
		long[] id =  { ++size_ };
		Key key = new Key(id);
		try {
			Field field = row.getClass().getField(config_.primary_keys[0].toString().toLowerCase());
			field.setLong(row, id[0]);
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
			System.exit(0);
		}
		AddForeignKeyIndices(key, row);
		UpdateRowInternal(key, row);
		return key;
	}
	
	public abstract RowType FindRow(Key key);
	
	public void UpdateRow(RowType row) {
		VerifyValue(row);
		Key key = GetKey(row);
		RowType old_row = FindRow(key);
		if (old_row != null) DeleteForeignKeyIndices(key, old_row);
		AddForeignKeyIndices(key, row);
		UpdateRowInternal(key, row);
	}
	
	protected abstract void UpdateRowInternal(Key key, RowType row);
	
	public RowType DeleteRow(Key key) {
		if (config_.key_owner) {
			System.err.println("This table is not delete-able.");
			System.exit(0);
			return null;
		}
		RowType old_row = DeleteRowInternal(key);
		if (old_row == null) return null;
		DeleteForeignKeyIndices(key, old_row);
		return old_row;
	}
	
	protected abstract void ClearInternal();
	
	public void Clear() {
		ClearInternal();
		foreign_key_maps_.clear();
		FileUtil.DeleteOneDir(config_.dir);
	}
	
	protected abstract RowType DeleteRowInternal(Key key);
	
	private class ValueIterator implements Iterator<RowType> {
		public ValueIterator(Iterator<Entry<Key, RowType>> iterator) {
			this.iterator = iterator;
		}
		@Override
		public boolean hasNext() {
			return iterator.hasNext();
		}

		@Override
		public RowType next() {
			return iterator.next().getValue();
		}
		
		private Iterator<Entry<Key, RowType>> iterator;
	}
	private class KeyIterator implements Iterator<Key> {
		public KeyIterator(Iterator<Entry<Key, RowType>> iterator) {
			this.iterator = iterator;
		}
		@Override
		public boolean hasNext() {
			return iterator.hasNext();
		}

		@Override
		public Key next() {
			return iterator.next().getKey();
		}
		
		private Iterator<Entry<Key, RowType>> iterator;
	}
	
	public abstract Iterator<Entry<Key, RowType>> GetIterator();
	
	public Iterator<RowType> GetRowIterator() {
		return new ValueIterator(GetIterator());
	}
	
	public Iterator<Key> GetKeyIterator() {
		return new KeyIterator(GetIterator());
	}
	
	public void Load() {
		size_ = LoadInternal();
	}
	
	public long GetSize() {
		return size_;
	}
	
	protected abstract long LoadInternal();
	
	public abstract void Flush();
	
	public boolean VerifyForeignKey(Long id) {
		if  (config_.key_owner && (id < 0 ||id > size_)) {
			System.err.println("Foreign Key " + config_.primary_keys[0].toString() + "(" + id + ") out of range (" +
					+ 0 + " - " + size_ + ")");
			return false;
		}
		return true;
	}
	
	public Key GetKey(RowType row) {
		try {
			long[] keys = new long[config_.primary_keys.length];
			for (int i = 0; i < keys.length; ++i) {
				Field field = row.getClass().getField(config_.primary_keys[i].toString().toLowerCase());
				keys[i] = field.getLong(row);
			}
			return new Key(keys);
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
			System.exit(0);
		}
		return null;
	}
	
	public boolean SetKey(RowType row, Key key) {
		if (key.keys.length != config_.primary_keys.length) {
			System.err.println("Unexpected nubmer of keys");
			return false;
		}
		try {
			for (int i = 0; i < key.keys.length; ++i) {
				Field field = row.getClass().getField(config_.primary_keys[i].toString().toLowerCase());
				field.setLong(row, key.keys[i]);
			}
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
			System.exit(0);
		}
		return true;
	}
	
	public void GetKeysByForeignKey(String field_name, long foreign_key, SortedSet<Key> row_keys) {
		TreeSet<Key> keys = GetKeysByForeignKey(field_name, foreign_key);
		row_keys.clear();
		if (keys != null) row_keys.addAll(keys);
	}
	
	public void FilterKeysByForeignKey(String field_name, long foreign_key, SortedSet<Key> row_keys) {
		TreeSet<Key> keys = GetKeysByForeignKey(field_name, foreign_key);
		if (keys != null) {
			row_keys.retainAll(keys);
		} else {
			row_keys.clear();
		}
	}
	
	private TreeSet<Key> GetKeysByForeignKey(String field_name, long foreign_key) {
		HashMap<Long, TreeSet<Key>> key_map = foreign_key_maps_.get(field_name);
		if (key_map == null) {  // Lazy Mode
			key_map = ConstructForeignKeyMap(field_name);
			foreign_key_maps_.put(field_name, key_map);
		}
		return key_map.get(foreign_key);
	}
	
	protected void VerifyValue(RowType row) {
		if (config_.foreign_keys == null) return;
		for (Entry<String, KeyType> foreign_key : config_.foreign_keys.entrySet()) {
			String field_name = foreign_key.getKey();
			Predicate<Long> verifier = foreign_key_verifiers_.get(field_name);
			if (verifier == null) {
				System.err.println("Foreign key verifier isn't found for " + field_name);
				System.exit(0);
			}
			if (!verifier.test(GetForeignKey(field_name, row))) {
				System.err.println("Foreign key doesn't exist for " + field_name);
				System.exit(0);
			}
		}
	}
	
	private HashMap<Long, TreeSet<Key>> ConstructForeignKeyMap(String field_name) {
		Flush();
		HashMap<Long, TreeSet<Key>> key_map = new HashMap<Long, TreeSet<Key>>();
		Iterator<Entry<Key, RowType>> iter = this.GetIterator();
		while (iter.hasNext()) {
			Entry<Key, RowType> e = iter.next();
			Long foreign_key = GetForeignKey(field_name, e.getValue());
			AddForeignKeyIndicesToMap(key_map, e.getKey(), foreign_key);
		}
		return key_map;
	}
	
	private void AddForeignKeyIndices(Key key, RowType row) {
		for (Entry<String, HashMap<Long, TreeSet<Key>>> map : foreign_key_maps_.entrySet()) {
			AddForeignKeyIndicesToMap(map.getValue(), key, GetForeignKey(map.getKey(), row));
		}
	}
	
	private void DeleteForeignKeyIndices(Key key, RowType row) {
		for (Entry<String, HashMap<Long, TreeSet<Key>>> map : foreign_key_maps_.entrySet()) {
			long foreign_key = GetForeignKey(map.getKey(), row);
			TreeSet<Key> keys = map.getValue().get(foreign_key);
			if (keys != null) keys.remove(key);
		}
	}
	
	private void AddForeignKeyIndicesToMap(HashMap<Long, TreeSet<Key>> key_map, Key key, long foreign_key) {
		TreeSet<Key> keys = key_map.get(foreign_key);
		if (keys == null) {
			keys = new TreeSet<Key>();
			key_map.put(foreign_key, keys);
		}
		keys.add(key);
	}
	
	private long GetForeignKey(String field_name, RowType row) {
		try {
			Field field = row.getClass().getField(field_name.toLowerCase());
			return field.getLong(row);
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
			System.exit(0);
		}
		return 0;
	}
	
	protected TableConfig config_;

	protected Class<?> row_type_class_;
	protected Map<String, Predicate<Long>> foreign_key_verifiers_;
	protected Shard shard_;
	protected FileIO<RowType> io_;
	protected HashMap<String, HashMap<Long, TreeSet<Key>>> foreign_key_maps_  =
			new HashMap<String, HashMap<Long, TreeSet<Key>>>();
	
	private long size_ = 0L;
}
