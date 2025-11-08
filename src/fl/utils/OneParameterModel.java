package fl.utils;

import org.apache.commons.math3.stat.regression.SimpleRegression;

// The class models Y = alpha * X ^ beta
public class OneParameterModel extends AbstractModel {
	@Override
	public void AddData(double y, double... x) {
		if (x.length != 1) {
			System.err.println("Got " + x.length + " vars for 1-param model.");
			return;
		}
		regression_.addData(Math.log(x[0]), Math.log(y));
		data_size_++;
	}
	
	@Override
	public boolean Process() {
		if (data_size_ < kMinSamples) return false;
		Regress();
		return true;
	}
	
	@Override
	public double Predict(double bias, double... x) {
		if (x.length != 1) {
			System.err.println("Got " + x.length + " vars for 1-param model.");
			return 0;
		}
		return Math.exp(alpha + GetBias(alpha_stderr, bias)
			+ Math.log(x[0]) * beta + GetBias(beta_stderr, bias));
	}
	
	private void Regress() {
		beta = regression_.getSlope();
		alpha = regression_.getIntercept();
		alpha_stderr = regression_.getInterceptStdErr();
		beta_stderr = regression_.getSlopeStdErr();
		if (Double.isNaN(beta) || Double.isNaN(alpha) || Double.isNaN(beta_stderr) || Double.isNaN(alpha_stderr)) {
			System.err.println("Model can not be trained");
			System.exit(0);
		}
	}
	
	private double GetBias(double stderr, double bias) {
		if (stderr == 0) return 0;
		return RandomUtil.InverseNormalDistributionCDF(0, stderr, bias);
	}
	
	public double alpha;
	public double beta;
	public double alpha_stderr;
	public double beta_stderr;
	private SimpleRegression regression_ = new SimpleRegression(true);
	private int data_size_;
	private int kMinSamples = 20;
}
