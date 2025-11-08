package fl.utils;

import java.util.Random;

import org.apache.commons.math3.distribution.NormalDistribution;

public class RandomUtil {
	public static boolean WhetherToHappend(double probability) {
		return random.nextFloat() < probability;
	}
	
	public static boolean DeterministicWhetherToHappend(double probability, long seed) {
		return new Random(seed).nextFloat() < probability;
	}
	
	public static double SampleFromLogNormalDistribution(double mean, double std_dev) {
		return Math.exp(random.nextGaussian() * std_dev) * mean;
	}	
	
	public static double SampleFromNormalDistribution(double mean, double std_dev) {
		return random.nextGaussian() * std_dev + mean;
	}
	
	public static int SampleFromUniformDistribution(int lowerbound, int upperbound) {
		return random.nextInt(upperbound - lowerbound + 1) + lowerbound;
	}
	
	public static double SampleFromUniformDistribution(double lowerbound, double upperbound) {
		return random.nextDouble() * (upperbound - lowerbound) + lowerbound;
	}
	
	public static int RepeatedBounolli(int times, double prob) {
		int ret = 0;
		for (int i = 0; i < times; ++i) {
			if (WhetherToHappend(prob)) ret++; 
		}
		return ret;
	}
	
	public static double PowerLaw(double rank, double k) {
		return Math.pow(1.0 / rank, k) - Math.pow(1.0 / (rank + 1.0), k);
	}
	
	public static double InverseNormalDistributionCDF(double mean, double sd, double p) {
		return new NormalDistribution(mean, sd).inverseCumulativeProbability(p);
	}
	
	public static double NormalDistributionCDF(double mean, double sd, double x0, double x1) {
		return new NormalDistribution(mean, sd).probability(x0, x1);
	}
	
//	public static double InverseNormalDistributionCDF(double p) {
//	    return (2 / Math.sqrt(2)) * InverseErrorFunction(2 * p - 1);
//	}
//	
//	private static double InverseErrorFunction(double z) {
//	    int nTerms = 315;
//	    double runningSum = 0;
//	    double[] a = new double[nTerms + 1];
//	    double[] c = new double[nTerms + 1];
//	    c[0]=1;
//	    for(int n = 1; n < nTerms; n++){
//	        double runningSum2 = 0;
//	        for (int k = 0; k <= n-1; k++){
//	            runningSum2 += c[k] * c[n-1-k] / ((k + 1) * (2 * k + 1));
//	        }
//	        c[n] = runningSum2;
//	        runningSum2 = 0;
//	    }
//	    for(int n = 0; n < nTerms; n++){
//	        a[n] = c[n] / (2 * n + 1);
//	        runningSum += a[n] * Math.pow((0.5) * Math.sqrt(Math.PI) * z, 2 * n + 1);
//	    }
//	    return runningSum;
//	}
	
	private static Random random = new Random();
}
