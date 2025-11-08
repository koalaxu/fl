package fl.db;

import fl.data.TableConfig;

public class SinlgeShard extends Shard {
	
	public SinlgeShard(TableConfig config) {
		super(config);
		long[] min = new long[config.primary_keys.length];
		long[] max = new long[config.primary_keys.length];
		for (int i = 0; i < config.primary_keys.length; ++i) {
			min[i] = 0;
			max[i] = Long.MAX_VALUE;
		}
		lowerbound = new Key(min);
		upperbound = new Key(max);
	}

	@Override
	public String GetFilePattern() {
		return "data\\." + file_suffix_;
	}

	@Override
	public Key GetShardKey(Key key) {
		return single_shard_key;
	}
	
	@Override
	public Key GetKeyLowerBound(Key shard_key) {
		return lowerbound;
	}
	
	@Override
	public Key GetKeyUpperBound(Key shard_key) {
		return upperbound;
	}
	
	@Override
	public Key GetShardKeyFromFileName(String filename) {
		return single_shard_key;
	}

	@Override
	public String GetFileNameFromShardKey(Key shard_key) {
		return "data." + file_suffix_;
	}
	
	@Override
	public int compare(Key o1, Key o2) {
		return o1.compareTo(o2);
	}
	
	static private long[] dummy_keys = { 0 }; 
	static private Key single_shard_key = new Key(dummy_keys);
	private Key lowerbound;
	private Key upperbound;

}
