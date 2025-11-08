package fl.utils;

import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;

public class WeightedSampler<E> {
    public WeightedSampler() {
        this(new Random());
    }

    public WeightedSampler(Random random) {
        this.random = random;
    }

    public WeightedSampler<E> Add(E result, double weight) {
        if (weight <= 0) return this;
        total += weight;
        map.put(total, result);
        return this;
    }
    
    public void AddAll(Map<E, Double> map) {
    	for (Entry<E, Double> entry : map.entrySet()) {
    		Add(entry.getKey(), entry.getValue());
    	}
    }

    public E Sample() {
    	if (map.isEmpty()) return null;
        double value = random.nextDouble() * total;
        return map.higherEntry(value).getValue();
    }
	
    private final NavigableMap<Double, E> map = new TreeMap<Double, E>();
    private final Random random;
    private double total = 0;
}
