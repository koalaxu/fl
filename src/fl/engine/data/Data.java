package fl.engine.data;

import fl.db.DataBase;

public class Data {
	protected Data(DataBase db) {
		this.db_ = db;
	}
	
	protected DataAccessor GetAccessor() {
		return DataAccessor.GetDataAccessor(db_);
	}
	
	protected DataBase db_;
}
