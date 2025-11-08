package fl.utils;

import fl.data.Global;

public class StringUtil {
	public static String GetTime(Global global) {
		return global.year + " - " + GetTime(global.week, global.mid_week);
	}
	
	public static String GetTime(int week, boolean mid_week) {
		return "Week #" + week + (mid_week ? " (2nd half)" : " (1st half)");
	}
	
	public static String PascalToUnderscore(String input) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < input.length(); ++i) {
			char ch = input.charAt(i);
			if (Character.isUpperCase(ch) && i > 0) {
				builder.append('_');
			}
			builder.append(Character.toLowerCase(ch));
		}
		return builder.toString();
	}
	
	public static String PercentageNumber(long numerator, long denominator) {
		return String.format("%.1f%%", numerator * 100.0 / denominator);
	}
	
	public static String Number(Integer value) {
		return String.format("%,d", value);
	}
	
	public static String ShortNumber(Integer value) {
		if (value == null) return "";
		if (Math.abs(value) >= 1000000) {
			return String.format("%.3fM", value.doubleValue() / 1000000);
		} else if (Math.abs(value) >= 1000) {
			return String.format("%.3fK", value.doubleValue() / 1000);
		}
		return value.toString();
	}
	
	public static String ShortNumber(Long value) {
		if (value == null) return "";
		if (Math.abs(value) >= 1000000) {
			return String.format("%.3fM", value.doubleValue() / 1000000);
		} else if (Math.abs(value) >= 1000) {
			return String.format("%.3fK", value.doubleValue() / 1000);
		}
		return value.toString();
	}
	
	public static String ShortTime(int value) {
		if (value == 0) return "";
		if (value > 9) {
			return String.format("%.1f Month", value * 3.5 / 30);
		}
		return String.format("%d Week", (value - 1) / 2 + 1);
	}
	
	public static String GameTime(int sec) {
		return String.format("%02d:%02d", sec / 60, sec % 60);
	}
}
