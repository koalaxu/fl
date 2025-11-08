package fl.data;

import java.io.Serializable;

public class ClubRecord implements Serializable  {
	private static final long serialVersionUID = 1L;
	public long club_id;
	public long year;

	public long income;
	public long expense;
	public long balance;
	public long domestic_fans;
	public long international_fans;
}
