package fl.utils;

public abstract class AbstractModel {
	public abstract void AddData(double y, double... x);
	
	public abstract boolean Process();
	
	public abstract double Predict(double bias, double... x);
}
