package fl.utils;

import java.util.List;

public class MathUtil {
	public static void Normalize(double[] weights) {
		double total_weight = 0;
		for (double weight : weights) {
			total_weight += weight;
		}
		for (int i = 0; i < weights.length; ++i) {
			weights[i] /= total_weight;
		}
	}
	
	public static void Normalize(List<Double> weights) {
		double total_weight = 0;
		for (double weight : weights) {
			total_weight += weight;
		}
		for (int i = 0; i < weights.size(); ++i) {
			weights.set(i, weights.get(i) / total_weight);
		}
	}
	
	public static long RoudByHundred(long input) {
		return RoundBy(input, 100);
	}
	
	public static long RoudByThousand(long input) {
		return RoundBy(input, 1000);
	}
	
	public static long RoundBy(long input, int granularity) {
		return Math.round(((double)input / granularity)) * granularity;
	}
}
