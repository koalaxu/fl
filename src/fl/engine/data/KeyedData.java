package fl.engine.data;

import fl.db.DataBase;
import fl.db.Key;

public class KeyedData extends Data {
	protected KeyedData(DataBase db, Key key) {
		super(db);
		key_ = key;
	}

	public Key GetKey() {
		return key_;
	}
	protected Key key_;
	
	@Override
	public int hashCode() {
		return key_.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		KeyedData other = (KeyedData) obj;
		return this.key_.equals(other.key_);
	}
}
