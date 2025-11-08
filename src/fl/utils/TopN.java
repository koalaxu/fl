package fl.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class TopN {
	public static <T> List<T> FindSortedTopN(Collection<T> input, int n, Comparator<T> comp) {
		PriorityQueue<T> heap = new PriorityQueue<T>(input.size(), comp.reversed());
		for (T t : input) {
			heap.add(t);
			if (heap.size() > n) {
				heap.poll();
			}
		}
		List<T> ret = new ArrayList<T>();
		while (!heap.isEmpty()) {
			ret.add(heap.poll());
		}
		Collections.reverse(ret);
		return ret;
	}
	
	public static <T> List<T> FindSortedTopNForScoredObject(
			Collection<ScoredObject<T>> input, int n, Comparator<ScoredObject<T>> comp) {
		List<T> ret = new ArrayList<T>();
		for (ScoredObject<T> st : FindSortedTopN(input, n, comp)) {
			ret.add(st.element);
		}
		return ret;
	}
}
