package fl.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;

public class MultiParameterModel extends AbstractModel {
	public MultiParameterModel(int num_variable) {
		num_variable_ = num_variable;
	}
	
	@Override
	public void AddData(double y, double... x) {
		if (x.length != num_variable_) {
			System.err.println("Invalid input: expect " + num_variable_ + "vars, got " + x.length);
			return;
		}
		data_.add(Math.log(y));
		for (double each_x : x) data_.add(Math.log(each_x));
	}
	
	@Override
	public boolean Process() {
		if (data_.size() < kMinSamples) return false;
		double[] data = new double[data_.size()];
		for (int i = 0; i < data_.size(); ++i) data[i] = data_.get(i);
		regression_.newSampleData(data, data.length / (num_variable_ + 1), num_variable_);
		betas = regression_.estimateRegressionParameters();
		try {
			betas_stderr = regression_.estimateRegressionParametersStandardErrors();
		} catch (Exception e) {
			betas_stderr = null;
		}
		return true;
	}
	
	@Override
	public double Predict(double bias, double... x) {
		if (x.length != num_variable_) {
			System.err.println("Invalid input: expect " + num_variable_ + "vars, got " + x.length);
			return 0;
		}
		double exponent = betas[0] + GetBetaBias(0, bias);
		for (int i = 0; i < num_variable_; ++i) {
			exponent += Math.log(x[i]) * (betas[i + 1] + GetBetaBias(i + 1, bias));
		}
		return Math.exp(exponent);
	}
	
	
	private double GetBetaBias(int index, double bias) {
		if (betas_stderr == null) return 0.0;
		return GetBias(betas_stderr[index], bias);
	}
	
	private double GetBias(double stderr, double bias) {
		return RandomUtil.InverseNormalDistributionCDF(0, stderr, bias);
	}
	
	public double[] betas;
	public double[] betas_stderr;
	private List<Double> data_ = new ArrayList<Double>();
	private int num_variable_;
	private OLSMultipleLinearRegression regression_ = new OLSMultipleLinearRegression();

	private static int kMinSamples = 20;
}
