package fl.utils;

public class FieldAccessor  {
	public FieldAccessor(int default_value, int min_value, int max_value) {
		this.default_value = default_value;
		this.min_value = min_value;
		this.max_value = max_value;
	}
	
	public int Get(Integer field) {
		if (field == null) return default_value;
		return field.intValue();
	}
	
	public Integer Set(int value) {
		if (value == default_value) return null;
		return Integer.valueOf(value);
	}
	
	public Integer Update(Integer field, int update_value) {
		int value = (field == null ? default_value : field.intValue()) + update_value;
		value = Math.max(min_value, Math.min(max_value, value));
		return Set(value);
	}	
	
	public static FieldAccessor kZeroBased = new FieldAccessor(0, Integer.MIN_VALUE, Integer.MAX_VALUE);
	public static FieldAccessor kHundredBased = new FieldAccessor(100, 0, 100);
	
	int default_value;
	int min_value;
	int max_value;
}
