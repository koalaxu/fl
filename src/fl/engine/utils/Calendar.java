package fl.engine.utils;

import java.util.TreeSet;
import java.util.SortedSet;

import fl.data.Event;
import fl.data.utils.JsonUtil;

public class Calendar {
	public Calendar() {
		JsonUtil.ParseArrayFromJson("constants/calendar.json", events_, Event.class, false);
	}
	
	public Event GetEvent(int week_id, boolean mid_week) {
		for (Event event : events_) {
			if (event.week_id == week_id && event.mid_week == mid_week) return event;
			if (event.week_id > week_id) break;
		}
		return null;
	}
	
	private SortedSet<Event> events_ = new TreeSet<Event>();
}
