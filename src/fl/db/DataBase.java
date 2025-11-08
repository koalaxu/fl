package fl.db;


import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.function.Predicate;

import fl.data.Club;
import fl.data.ClubInfo;
import fl.data.ClubRecord;
import fl.data.Country;
import fl.data.DataTest;
import fl.data.League;
import fl.data.LeagueInfo;
import fl.data.LeagueRecord;
import fl.data.Match;
import fl.data.MatchCommentary;
import fl.data.Player;
import fl.data.PlayerHistory;
import fl.data.PlayerInfo;
import fl.data.PlayerLeagueInfo;
import fl.data.PlayerLeagueRecord;
import fl.data.TableConfig;
import fl.data.TableConfig.KeyType;
import fl.data.Transfer;
import fl.data.utils.JsonUtil;
import fl.utils.StringUtil;

public class DataBase {
	public DataBase(String dir) {
		dir_ = dir;
		TableConfig[] table_configs = JsonUtil.ParseArrayFromJson("constants/schema.json", TableConfig[].class);
		ValidateTableConfigs(table_configs);
		// Topological Sorting
		while (table_configs_.size() < table_configs.length) {
			for (TableConfig config : table_configs) {
				if (table_config_map_.containsKey(config.name)) continue;
				boolean all_parents_ready = true;
				if (config.parent_tables != null) {
					for (String parent : config.parent_tables) {
						if (!table_config_map_.containsKey(parent)) {
							all_parents_ready = false;
							break;
						}
					}
				}
				if (all_parents_ready) {
					table_config_map_.put(config.name, config);
					table_configs_.add(config);
				}
			}
		}
	}
	
	public void Load() {
		HashMap<KeyType, Predicate<Long>> table_key_predicates = new HashMap<KeyType, Predicate<Long>>();
		table_key_predicates.put(KeyType.ROUND_ID, id -> { return true; });
		table_key_predicates.put(KeyType.MATCH_ID, id -> { return true; });
		table_key_predicates.put(KeyType.YEAR, id -> { return true; });
		for (TableConfig config : table_configs_) {
			Field table_field = null;
		    for (Field field : DataBase.class.getDeclaredFields()) {
		    	if (StringUtil.PascalToUnderscore(config.name + "Table").equals(field.getName())) {
		    		table_field = field;
		    		break;
		    	}
		    }
		    if (table_field == null) {
		    	System.err.println("Table " + config.name + " can't find a corresponding field.");
		    	System.exit(0);
		    }
			Type generic_type = table_field.getGenericType();
			Class<?> row_type = null;
			if(generic_type instanceof ParameterizedType){
				ParameterizedType ptype = (ParameterizedType)generic_type;
				if (!(ptype.getRawType() == Table.class)) continue;
				row_type = (Class<?>)ptype.getActualTypeArguments()[0];
			}
			if (row_type == null || !config.row_type.equals(row_type.getSimpleName())) {
				System.err.println("Table " + config.name + " doesn't have a parameterized type matching row type");
				System.exit(0);
			}
			Method method;
			try {
				method = table_field.getType().getMethod("BuildTable", TableConfig.class, Class.class, Map.class);
				Map<String, Predicate<Long>> predicates = new TreeMap<String, Predicate<Long>>();
				if (config.foreign_keys != null) {
					for (Entry<String, KeyType> foreign_key : config.foreign_keys.entrySet()) {
						Predicate<Long> predicate = table_key_predicates.get(foreign_key.getValue());
						if (predicate == null) {
							System.err.println("The foreign key " + foreign_key + " doesn't have a table");
							System.exit(0);
						}							
						predicates.put(foreign_key.getKey(), predicate);
					}
				}
				Table<?> table = (Table<?>) method.invoke(null, config, row_type, predicates);
				table_field.set(this, table);
				tables_.put(config.name, table);
				System.out.println("Loading " + config.name + " table ...");
				table.Load();
				if (config.key_owner) {
					table_key_predicates.put(config.primary_keys[0], table::VerifyForeignKey);
				}
			} catch (NoSuchMethodException | SecurityException | IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
				e.printStackTrace();
				System.exit(0);
			}
		}
	}
	
	public void Flush() {
		for (Table<?> table : tables_.values()) {
			table.Flush();
		}
	}
	
	private void ValidateTableConfigs(TableConfig[] configs) {
		HashSet<String> table_names = new HashSet<String>();
		for (TableConfig config : configs) {
			config.dir = dir_ + config.dir;
			table_names.add(config.name);
		}
		for (TableConfig config : configs) {
			if (config.parent_tables == null) continue;
			for (String parent : config.parent_tables) {
				// Check if it's cycle dependency
				if (parent == config.name) {
					System.err.println(config.name + " depends on its self.");
					System.exit(0);
				}
				// Check if their parent table(s) exists
				if (!table_names.contains(parent)) {
					System.err.println(config.name + " has a non-existing parent " + parent);
					System.exit(0);
				}
			}
		}
	}
	
	public Table<Country> country_table;
	public Table<League> league_table;
	public Table<Club> club_table;
	public Table<Player> player_table;
	public Table<LeagueInfo> league_info_table;
	public Table<ClubInfo> club_info_table;
	public Table<PlayerInfo> player_info_table;
	public Table<Transfer> transfer_table;
	public Table<ClubRecord> club_history_table;
	public Table<PlayerLeagueInfo> player_league_info_table;
	public Table<PlayerLeagueRecord> player_league_history_table;
	public Table<PlayerHistory> player_history_table;
	public Table<LeagueRecord> league_history_table;
	public Table<Match> match_table;
	public Table<MatchCommentary> match_commentary_table;
	public Table<MatchCommentary> match_commentary_archive_table;
	public Table<DataTest> data_test_table; 
	
	public HashMap<String, Table<?>> tables_ = new HashMap<String, Table<?>>();
	private List<TableConfig> table_configs_ = new ArrayList<TableConfig>();
	private HashMap<String, TableConfig> table_config_map_ = new HashMap<String, TableConfig>();
	
	private String dir_;
};