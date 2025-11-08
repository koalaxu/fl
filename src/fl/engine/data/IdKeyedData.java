package fl.engine.data;

import fl.db.DataBase;

public abstract class IdKeyedData extends Data implements Comparable<IdKeyedData> {
	protected IdKeyedData(DataBase db, long key) {
		super(db);
		key_ = key;
	}
	
	public long GetKey() {
		return key_;
	}
	protected long key_;
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (key_ ^ (key_ >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IdKeyedData other = (IdKeyedData) obj;
		if (key_ != other.key_)
			return false;
		return true;
	}
	
	@Override
	public int compareTo(IdKeyedData o) {
		if (key_ > o.key_) return 1;
		if (key_ < o.key_) return -1;
		return 0;
	}
}
