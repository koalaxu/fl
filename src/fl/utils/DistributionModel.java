package fl.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

public class DistributionModel extends AbstractModel {
	public DistributionModel(int granularity) {
		granularity_ = granularity;
	}

	@Override
	public void AddData(double y, double... x) {
		if (x.length != 1) {
			System.err.println("Got " + x.length + " vars for 1-param model.");
			return;
		}
		int bucket = (int) (granularity_ * x[0]);
		List<Double> list = data_.get(bucket);
		if (list == null) {
			list = new ArrayList<Double>();
			data_.put(bucket, list);
		}
		list.add(y);
	}

	@Override
	public boolean Process() {
		if (data_.isEmpty()) return false;
		for (Entry<Integer, List<Double>> e : data_.entrySet()) {
			double[] distribution = QuantileCalculator.Compute(QuantileCalculator.kDeciles, e.getValue());
			model.put(e.getKey(), distribution);
		}
		return true;
	}
	
	@Override
	public double Predict(double bias, double... x) {
		if (x.length != 1) {
			System.err.println("Got " + x.length + " vars for 1-param model.");
			return 0;
		}
		int bucket = (int) (granularity_ * x[0]);
		double[] distribution = model.get(bucket);
		return distribution[(int) Math.round(bias * 10)];
	}
	
	public SortedMap<Integer, double[]> model = new TreeMap<Integer, double[]>();
	private SortedMap<Integer, List<Double>> data_ = new TreeMap<Integer, List<Double>>();
	private int granularity_;

}
