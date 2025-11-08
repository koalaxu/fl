package fl.data.utils;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class JsonUtil {
	
	@SuppressWarnings("unchecked")
	static public <T> T DeepCopy(T obj, Class<?> classOfT) {
		return (T) gson.fromJson(gson.toJson(obj), classOfT);
	}
	
	static public <T> T ParseOneObjectFromJson(String relative_path, Class<T> classOfT) {
		return gson.fromJson(GetFileReader(relative_path, false), classOfT);
	}
	
	static public <T> T[] ParseArrayFromJson(String relative_path, Class<T[]> classOfT) {
		return gson.fromJson(GetFileReader(relative_path, false), classOfT);
	}
	
	static public <T> void ParseArrayFromJson(
			String relative_path, Collection<T> array, Class<?> classOfT, boolean safe_load) {
		JsonReader reader = GetJsonReader(relative_path, safe_load);
		if (safe_load && reader == null) return;
		try {
			reader.beginArray();
			while (reader.hasNext()) {
				T e = gson.fromJson(reader, classOfT);
				array.add(e);
			}
			reader.endArray();
	        reader.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	static public String WriteOneObjectToJson(Object obj) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		return gson.toJson(obj);
	}	
	
	static public void WriteOneObjectToJson(String relative_path, Object obj) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		FileWriter writer = GetFileWriter(relative_path);
		gson.toJson(obj, obj.getClass(), writer);
		try {
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	static public <T> void WriteArrayToJson(
			String relative_path, Collection<T> array, Class<?> classOfT, boolean safe_write) {
		if (array.isEmpty() && safe_write) {
			FileUtil.DeleteOneFile(relative_path);
			return;
		}
		JsonWriter writer = GetJsonWriter(relative_path);
		writer.setIndent("  ");
		writer.setLenient(true);
		try {
	        writer.beginArray();
	        for (T e : array) {
	            gson.toJson(e, classOfT, writer);
	        }
	        writer.endArray();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	static private FileReader GetFileReader(String relative_path, boolean safe_load) {
		FileReader reader = null;
		try {
			reader = new FileReader(FileUtil.GetFilePath(relative_path));
		} catch (FileNotFoundException e) {
			if (!safe_load) {
				e.printStackTrace();
				System.exit(0);
			}
		}
		return reader;
	}
	
	static private FileWriter GetFileWriter(String relative_path) {
		FileWriter writer = null;
		try {
			writer = new FileWriter(FileUtil.GetFilePath(relative_path));
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
		return writer;
	}
	
	static private JsonWriter GetJsonWriter(String relative_path) {
		JsonWriter writer = null;
		try {
			writer = new JsonWriter(new FileWriter(FileUtil.CreateOrReplaceOneFile(relative_path)));
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
		return writer;
	}
	
	static private JsonReader GetJsonReader(String relative_path, boolean safe_load) {
		FileReader reader = GetFileReader(relative_path, safe_load);
		if (reader == null) return null;
		return new JsonReader(reader);
	}
	static private Gson gson = new Gson();
	//static private Gson gson = new GsonBuilder().setPrettyPrinting().create();
}
