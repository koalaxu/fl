package fl.db;

import java.util.Collection;

import fl.data.utils.JsonUtil;

public class JsonIO<T> implements FileIO<T> {

	@Override
	public void Load(String relative_path, Collection<T> array, Class<?> classOfT, boolean safe_load) {
		JsonUtil.ParseArrayFromJson(relative_path, array, classOfT, safe_load);
	}

	@Override
	public void Save(String relative_path, Collection<T> array, Class<?> classOfT, boolean safe_save) {
		JsonUtil.WriteArrayToJson(relative_path, array, classOfT, safe_save);
	}
}
