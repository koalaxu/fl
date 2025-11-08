package fl.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Predicate;

public class ScoredObject<T> implements Comparable<ScoredObject<T>> {
	public ScoredObject(T element, double score) {
		this.element = element;
		this.score = score;
	}
	public T element;
	public double score;
	
	@Override
	public int compareTo(ScoredObject<T> o) {
		if (score > o.score) return 1;
		if (score < o.score) return -1;
		return 0;
	}
	
	public static <T, S> List<ScoredObject<T>> MapToScoredList(Map<T, S> input) {
		List<ScoredObject<T>> output = new ArrayList<ScoredObject<T>>();
		for (Entry<T, S> entry : input.entrySet()) {
			output.add(new ScoredObject<T>(entry.getKey(), (double) entry.getValue()));
		}
		return output;
	}
	
	public static <T> void CreateScoredList(
			Collection<T> list, Collection<ScoredObject<T>> scored_list, Function<T, Double> scoring_func) {
		for (T t : list) {
			scored_list.add(new ScoredObject<T>(t, scoring_func.apply(t)));
		}
	}
	
	public static <T> void CreateScoredList(
			T[] list, Collection<ScoredObject<T>> scored_list, Function<T, Double> scoring_func) {
		for (T t : list) {
			scored_list.add(new ScoredObject<T>(t, scoring_func.apply(t)));
		}
	}
	
	public static <T> void ScoredListToList(Collection<ScoredObject<T>> scored_list, Collection<T> list) {
		for (ScoredObject<T> t : scored_list) {
			list.add(t.element);
		}
	}
	
	public static <T> ScoredObject<T> GetMin(
			Collection<T> list, Function<T, Double> scoring_func, Predicate<T> filter) {
		T ret = null;
		double min_score = Double.MAX_VALUE;
		for (T t : list) {
			if (filter != null && filter.test(t)) continue;
			double score = scoring_func.apply(t);
			if (score < min_score) {
				min_score = score;
				ret = t;
			}
		}
		return new ScoredObject<T>(ret, min_score);
	}
	
	public static <T> ScoredObject<T> GetMax(
			Collection<T> list, Function<T, Double> scoring_func, Predicate<T> filter) {
		T ret = null;
		double max_score = Double.MIN_VALUE;
		for (T t : list) {
			if (filter != null && filter.test(t)) continue;
			double score = scoring_func.apply(t);
			if (score > max_score) {
				max_score = score;
				ret = t;
			}
		}
		return new ScoredObject<T>(ret, max_score);
	}
	
	public static <T> ScoredObject<T> GetMin(T[] list, Function<T, Double> scoring_func, Predicate<T> filter) {
		T ret = null;
		double min_score = Double.MAX_VALUE;
		for (T t : list) {
			if (filter != null && filter.test(t)) continue;
			double score = scoring_func.apply(t);
			if (score < min_score) {
				min_score = score;
				ret = t;
			}
		}
		return new ScoredObject<T>(ret, min_score);
	}
	
	public static <T> ScoredObject<T> GetMax(T[] list, Function<T, Double> scoring_func, Predicate<T> filter) {
		T ret = null;
		double max_score = Double.MIN_VALUE;
		for (T t : list) {
			if (filter != null && filter.test(t)) continue;
			double score = scoring_func.apply(t);
			if (score > max_score) {
				max_score = score;
				ret = t;
			}
		}
		return new ScoredObject<T>(ret, max_score);
	}
	
	private static class SortByScore<T> implements Comparator<ScoredObject<T>> {
		public SortByScore(boolean descending) {
			this.descending = descending;
		}
		@Override
		public int compare(ScoredObject<T> o1, ScoredObject<T> o2) {
			return o1.compareTo(o2) * (descending ? -1 : 1);
		}
		
		private boolean descending;
	}
	
	static public <T> Comparator<ScoredObject<T>> SortByScore() {
		return new SortByScore<T>(true);
	}
	
	static public <T> Comparator<ScoredObject<T>> SortByScoreAscending() {
		return new SortByScore<T>(false);
	}
}
