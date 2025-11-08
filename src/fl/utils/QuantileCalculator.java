package fl.utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

public class QuantileCalculator {
	public QuantileCalculator(double[] quantiles) {
		this.quantiles = quantiles;
		elements = new ArrayList<Double>();
	}
	
	public QuantileCalculator(double[] quantiles, int estimated_size) {
		this.quantiles = quantiles;
		elements = new ArrayList<Double>(estimated_size);
	}
	
	public void AddOneElement(double value) {
		elements.add(value);
	}
	
	public double[] Compute() {
		return Compute(quantiles, elements);
	}
	
	public double[] GetQuantileOrder() {
		return quantiles;
	}
	
	public int GetSize() {
		return elements.size();
	}
	
	public double GetMean() {
		if (elements.isEmpty()) return Double.NaN;
		double sum = 0.0;
		for (double e : elements) {
			sum += e;
		}
		return sum / elements.size();
	}
	
	public static <T> double[] Compute(
			double[] desired_quantiles, Iterable<T> dataSet, Function<T, Double> func, int estimated_size) {
		QuantileCalculator quantile_cal = estimated_size > 0 ?
				new QuantileCalculator(desired_quantiles, estimated_size) : new QuantileCalculator(desired_quantiles);
		for (T data : dataSet) {
			quantile_cal.AddOneElement(func.apply(data));
		}
		return quantile_cal.Compute();
	}
	
	public static double[] Compute(double[] desired_quantiles, List<Double> list) {
		list.sort(Comparator.naturalOrder());
		double[] ret = new double[desired_quantiles.length];
		int size = list.size() - 1;
		for (int i = 0; i < desired_quantiles.length; ++i) {
			double pos = size * desired_quantiles[i];
			int lower_bound = (int)pos;
			int upper_bound = (int)Math.ceil(pos);
			if (lower_bound == upper_bound) {
				ret[i] = list.get(lower_bound);
				continue;
			}
			ret[i] = list.get(lower_bound) +
					(list.get(upper_bound) - list.get(lower_bound)) * (pos - lower_bound);
		}
		return ret;
	}
	
	public static double[] kDeciles = { 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9 };
	public static double[] kNormalPercentiles = { 0.01, 0.02, 0.05, 0.1, 0.25, 0.5, 0.75, 0.9, 0.95, 0.98, 0.99 };
	
	private double[] quantiles;
	private List<Double> elements;
}
