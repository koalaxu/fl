package fl.db;

import java.util.Comparator;

import fl.data.TableConfig;

public abstract class Shard implements Comparator<Key> {
	public Shard(TableConfig config) {
		config_ = config;
		if (config.file_format == TableConfig.FileFormat.JSON) {
			file_suffix_ = "json";
		} else if (config.file_format == TableConfig.FileFormat.BINARY) {
			file_suffix_ = "bin";
		}
	}
	public abstract String GetFilePattern();
	public abstract Key GetShardKey(Key key);
	public abstract Key GetKeyLowerBound(Key shard_key);
	public abstract Key GetKeyUpperBound(Key shard_key);
	public abstract Key GetShardKeyFromFileName(String filename);
	public abstract String GetFileNameFromShardKey(Key shard_key);
	
	
	protected TableConfig config_;
	protected String file_suffix_;
}
