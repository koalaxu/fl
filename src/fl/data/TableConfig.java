package fl.data;

import java.util.Map;

public class TableConfig {
	public String name;
	public String row_type;
	public String dir;
	public String[] parent_tables;
	public enum KeyType {
		NONE,
		PLAYER_ID,
		CLUB_ID,
		LEAGUE_ID,
		COUNTRY_ID,
		ROUND_ID,
		MATCH_ID,
		YEAR,
	}
		
	public KeyType[] primary_keys;
	
	public Map<String, KeyType> foreign_keys;
	
	public enum ShardingMethod {
		SINGLE_FILE,
		ID_PARTITION,
	}
	
	public ShardingMethod sharding_method;
	public int[] key_partition_sizes;
	
	public enum LoadingMethod {
		MEMORY,
		DISK,
	}
	public LoadingMethod loading_method;
	
	public enum FileFormat {
		JSON,
		BINARY,
	}
	public FileFormat file_format = FileFormat.JSON;
	
	public boolean key_owner;
}