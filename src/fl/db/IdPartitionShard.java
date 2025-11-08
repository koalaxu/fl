package fl.db;

import fl.data.TableConfig;

public class IdPartitionShard extends Shard {
	
	public IdPartitionShard(TableConfig config) {
		super(config);
		key_partition_sizes_ = new int[config_.primary_keys.length];
		shard_digits_ = new int[config_.primary_keys.length];
		for (int i = 0; i < config_.primary_keys.length; ++i) {
			key_partition_sizes_[i] = i < config_.key_partition_sizes.length ?
				config_.key_partition_sizes[i] : 0;
			shard_digits_[i] = GetShardDigits(config_.primary_keys[i]);
		}
	}
	
	@Override
	public String GetFilePattern() {
		String file_pattern = "data";
		for (int i = 0; i < key_partition_sizes_.length; ++i) {
			if (key_partition_sizes_[i] <= 0) continue;
			file_pattern = file_pattern + "-";
			for (int j = 0; j < shard_digits_[i]; ++j) {
				file_pattern = file_pattern + "[0-9]";
			}
		}
		return file_pattern + "\\." + file_suffix_;
	}

	@Override
	public Key GetShardKey(Key key) {
		long[] shard_keys = new long[key.keys.length];
		for (int i = 0; i < shard_keys.length; ++i) {
			shard_keys[i] = key_partition_sizes_[i] <= 0 ? 0 : key.keys[i] / key_partition_sizes_[i];
		}
		return new Key(shard_keys);
	}

	@Override
	public Key GetKeyLowerBound(Key shard_key) {
		long[] keys = new long[shard_key.keys.length];
		for (int i = 0; i < keys.length; ++i) {
			keys[i] = key_partition_sizes_[i] <= 0 ? 0 : shard_key.keys[i] * key_partition_sizes_[i];
		}
		return new Key(keys);
	}

	@Override
	public Key GetKeyUpperBound(Key shard_key) {
		long[] keys = new long[shard_key.keys.length];
		for (int i = 0; i < keys.length; ++i) {
			keys[i] = key_partition_sizes_[i] <= 0 ? Long.MAX_VALUE :
				(shard_key.keys[i] + 1) * key_partition_sizes_[i] - 1;
		}
		return new Key(keys);
	}

	@Override
	public Key GetShardKeyFromFileName(String filename) {
		long[] shard_keys = new long[key_partition_sizes_.length];
		int str_offset = 4;  // skip "data"
		for (int i = 0; i < key_partition_sizes_.length; ++i) {
			shard_keys[i] = 0;
			if (key_partition_sizes_[i] <= 0) continue;
			str_offset++;  // skip "-"
			String digits = filename.substring(str_offset, str_offset + shard_digits_[i]);
			str_offset += shard_digits_[i];  // skip digits
			shard_keys[i] = Long.valueOf(digits).longValue();
		}
		return new Key(shard_keys);
	}

	@Override
	public String GetFileNameFromShardKey(Key shard_key) {
		String filename = "data";
		for (int i = 0; i < key_partition_sizes_.length; ++i) {
			if (key_partition_sizes_[i] <= 0) continue;
			filename += String.format("-%0" + shard_digits_[i] + "d", shard_key.keys[i]);
		}
		return filename + "." + file_suffix_;
	}
	
	@Override
	public int compare(Key o1, Key o2) {
		int result = GetShardKey(o1).compareTo(GetShardKey(o2));
		if (result == 0) {
			result = o1.compareTo(o2);
		}
		return result;
	}
	
	private int GetShardDigits(TableConfig.KeyType key_type) {
		if (key_type == TableConfig.KeyType.YEAR) return 4;
		if (key_type == TableConfig.KeyType.LEAGUE_ID) return 2;
		return 5;
	}
	
	private int[] key_partition_sizes_;
	private int[] shard_digits_;
}
