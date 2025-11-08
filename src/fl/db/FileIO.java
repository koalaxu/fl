package fl.db;

import java.util.Collection;

public interface FileIO<T> {
	public void Load(String relative_path, Collection<T> array, Class<?> classOfT, boolean safe_load);
	public void Save(String relative_path, Collection<T> array, Class<?> classOfT, boolean safe_save);
}
