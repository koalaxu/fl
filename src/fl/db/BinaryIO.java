package fl.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Collection;

import fl.data.utils.FileUtil;

public class BinaryIO<T> implements FileIO<T> {

	@SuppressWarnings("unchecked")
	@Override
	public void Load(String relative_path, Collection<T> array, Class<?> classOfT, boolean safe_load) {
		try {
			InputStream fis = new FileInputStream(FileUtil.GetFilePath(relative_path));
			ObjectInputStream in = new ObjectInputStream(fis);  
			Integer size = (Integer) in.readObject();
			for (int i = 0; i < size; ++i) {
				array.add((T)in.readObject());
			}
			fis.close();
		} catch (FileNotFoundException e) {
			if (safe_load) return;
			e.printStackTrace();
			System.exit(0);
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	@Override
	public void Save(String relative_path, Collection<T> array, Class<?> classOfT, boolean safe_save) {
		if (array.isEmpty() && safe_save) {
			FileUtil.DeleteOneFile(relative_path);
			return;
		}
		File f = FileUtil.CreateOrReplaceOneFile(relative_path);
		try {
			OutputStream fos = new FileOutputStream(f, false);
			ObjectOutputStream out = new ObjectOutputStream(fos);
			Integer size = array.size();
			out.writeObject(size);
			for (T t : array) {
				out.writeObject(t);
			}
			out.flush();
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
}
