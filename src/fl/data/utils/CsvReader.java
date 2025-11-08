package fl.data.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CsvReader {
	public static void ReadCsv(String relative_path, List<String[]> rows) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(FileUtil.GetFilePath(relative_path)));
			String line;
			while ((line = br.readLine()) != null) {
				String[] values = line.split(",");
				rows.add(values);
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	public static void ReadPairs(String relative_path, Map<String, String> map) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(FileUtil.GetFilePath(relative_path)));
			String line;
			while ((line = br.readLine()) != null) {
				String[] values = line.split(",");
				if (values.length != 2) {
					System.err.println("Wrong CSV row to read pairs: " + line);
					System.exit(0);
				}
				map.put(values[0], values[1]);
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	public static List<String> ReadSingleColumnCsv(String relative_path) {
		List<String> ret = new ArrayList<String>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(FileUtil.GetFilePath(relative_path)));
			String line;
			while ((line = br.readLine()) != null) {
				ret.add(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
		return ret;
	}
}
