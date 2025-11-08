package fl.data.utils;

import java.io.File;
import java.io.IOException;
import java.util.SortedSet;
import java.util.TreeSet;

public class FileUtil {
	// This function must be called first in the main.
	public static void Init(String base_path) {
		FileUtil.base_path = base_path;
	}
	
	public static void Init() {
		FileUtil.base_path = GetAbsolutePath() + "/data/";
	}
	
	public static String GetAbsolutePath() {
		File dir = new File("");
		return dir.getAbsolutePath();
	}
	
	public static String GetFilePath(String relative_path) {
		return base_path + relative_path;
	}
	
	public static boolean CheckExistence(String relative_path) {
		File file = new File(GetFilePath(relative_path));
		return file.exists();
	}
	
	public static File CreateOrReplaceOneFile(String relative_path) {
		File file = new File(GetFilePath(relative_path));
		File dir = new File(file.getParent());
		if (file.exists()) {
			file.delete();
		} else if (!dir.exists()) {
			dir.mkdirs();
		}		
		try {
			file.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
		return file;
	}
	
	public static void DeleteOneFile(String relative_path) {
		File file = new File(GetFilePath(relative_path));
		DeleteFile(file);
	}
	
	public static SortedSet<String> ListFile(String relative_path, String file_pattern) {
		SortedSet<String> files = new TreeSet<String>();
		File dir = new File(GetFilePath(relative_path));
		if (!dir.exists()) dir.mkdirs();
		for (File file :dir.listFiles()) {
			if (file.getName().matches(file_pattern)) {
				files.add(file.getName());
			}
		}
		return files;
	}
	
	public static void DeleteOneDir(String relative_path) {
		File dir = new File(GetFilePath(relative_path));
		RecursivelyDelete(dir);
	}
	
	private static void RecursivelyDelete(File file) {
		if (!file.exists()) return;
		if (file.isFile()) file.delete();
		if (file.isDirectory()) {
			for (File child : file.listFiles()) {
				RecursivelyDelete(child);
			}
			file.delete();
		}
	}
	
	private static void DeleteFile(File file) {
		if (!file.exists()) return;
		if (file.isFile()) file.delete();
	}
	
	
	private static String base_path;
}
