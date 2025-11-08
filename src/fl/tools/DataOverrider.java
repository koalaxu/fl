package fl.tools;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import fl.data.ClubInfo;
import fl.data.Player;
import fl.data.PlayerInfo;
import fl.data.utils.CsvReader;
import fl.data.utils.FileUtil;
import fl.db.DataBase;
import fl.db.Key;

public class DataOverrider {
	private DataOverrider(String dir) {
		db_ = new DataBase(dir);
		db_.Load();
	}
	
	private void OverrideClubs() {
		final String file_path = "club_override.csv";
		if (!FileUtil.CheckExistence(file_path)) {
			System.err.println(file_path + " doesn't exisit. Do not override clubs.");
			return;
		}
		List<String[]> rows = new ArrayList<String[]>();
		CsvReader.ReadCsv(file_path, rows);
		String[] header = rows.get(0);
		if (!header[0].equals("club_id")) {
			System.err.println("The first column must be club_id");
			System.exit(0);
		}
		for (int i = 1; i < rows.size(); ++i) {
			String[] values = rows.get(i);
			long club_id = Long.parseLong(values[0]);
			ClubInfo club_info = db_.club_info_table.FindRow(new Key(club_id));
			OverrideObject(club_info, null, header, values);
			db_.club_info_table.UpdateRow(club_info);
		}
	}
	
	private void OverridePlayers() {
		final String file_path = "player_override.csv";
		if (!FileUtil.CheckExistence(file_path)) {
			System.err.println(file_path + " doesn't exisit. Do not override clubs.");
			return;
		}
		List<String[]> rows = new ArrayList<String[]>();
		CsvReader.ReadCsv(file_path, rows);
		String[] header = rows.get(0);
		if (!header[0].equals("player_id")) {
			System.err.println("The first column must be player_id");
			System.exit(0);
		}
		for (int i = 1; i < rows.size(); ++i) {
			String[] values = rows.get(i);
			long player_id = Long.parseLong(values[0]);
			Player player = db_.player_table.FindRow(new Key(player_id));
			PlayerInfo player_info = db_.player_info_table.FindRow(new Key(player_id));
			OverrideObject(player_info, player, header, values);
			db_.player_table.UpdateRow(player);
			db_.player_info_table.UpdateRow(player_info);
		}
	}
	
	private void OverrideObject(Object obj0, Object obj1, String[] header, String[] values) {
		if (header.length != values.length) {
			System.err.println("Malformed CSV File: " +
					"header size = " + header.length + " value size = " + values.length);
			System.exit(0);
		}
		for (int i = 0; i < header.length; ++i) {
			if (OverrideOneObject(obj0, header[i], values[i])) continue;
			if (obj1 != null && OverrideOneObject(obj1, header[i], values[i])) continue;
			System.err.println("Unknown field: " + header[i]);
			System.exit(0);
		}
	}
	
	private boolean OverrideOneObject(Object obj, String field_name, String value) {
		try {
			String[] field_names = field_name.split("\\.");
			Field field = null;
			try {
				field = obj.getClass().getField(field_names[0]);
			} catch (NoSuchFieldException e) {
				return false;
			}
			if (field_names.length > 1) {
				return OverrideOneObject(field.get(obj), field_names[1], value);
			}
			if (field == null) return false;
			Type type = field.getGenericType();
			if (type == long.class) {
				field.setLong(obj, Long.parseLong(value));
			} else if (type == int.class) {
				field.setInt(obj, Integer.parseInt(value));
			} else {
				if (value.isEmpty()) {
					field.set(obj, null);
					return true;
				}
				if (type == Long.class) {
					field.set(obj, Long.valueOf(value));
				} else if (type == Integer.class) {
					field.set(obj, Integer.valueOf(value));
				} else if (type == String.class) {
					field.set(obj, value);
				} else if (Class.forName(type.getTypeName()).isEnum()) {
					Class<?> enum_class = Class.forName(type.getTypeName());
					try {
						int enum_value = Integer.valueOf(value).intValue();
						field.set(obj, enum_class.getEnumConstants()[enum_value]);
					} catch (NumberFormatException e) {
						field.set(obj, enum_class.getMethod("valueOf", String.class).invoke(null, value.toUpperCase()));
					}
				} else {
					System.err.println("Unsupported type: " + type);
					System.exit(0);
				}
			}
		} catch (SecurityException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
			return false;
		} catch (ClassNotFoundException | InvocationTargetException | NoSuchMethodException e) {
			e.printStackTrace();
			System.exit(0);
		}
		return true;
	}
	
	private void Done() {
		db_.Flush();
	}
	
	
	public static void main(String[] args) {
		FileUtil.Init();
		String dir = "test/";
		if (args.length > 0) {
			dir = args[0] + "/";
		}
		DataOverrider overrider = new DataOverrider(dir);
		overrider.OverrideClubs();
		overrider.OverridePlayers();
		overrider.Done();
	}
	
	private DataBase db_;
}
