package fl.engine.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import fl.data.Country;
import fl.data.utils.CsvReader;
import fl.utils.WeightedSampler;

public class ProbabilisticNameGenerator implements NameGenerator {
	public ProbabilisticNameGenerator(String first_initial_path, String last_name_path) {
		TreeMap<String, String> first_name_distribution = new TreeMap<String, String>();
		CsvReader.ReadPairs(first_initial_path, first_name_distribution);
		for (Entry<String, String> e : first_name_distribution.entrySet()) {
			first_name_sampler.Add(e.getKey(), Double.valueOf(e.getValue()));
		}
		List<String[]> last_name_distribution = new ArrayList<String[]>();
		CsvReader.ReadCsv(last_name_path, last_name_distribution);
		for (String[] e : last_name_distribution) {
			if (e.length != 3) {
				System.err.println("Expect 3 columns for last name csv");
				System.exit(0);
			}
			String country_name = e[0];
			String last_name = e[1];
			double weight = Double.valueOf(e[2]).doubleValue();
			if (!last_name_samplers.containsKey(country_name))
				last_name_samplers.put(country_name, new WeightedSampler<String>());
			last_name_samplers.get(country_name).Add(last_name, weight);
		}
	}
	
	@Override
	public String GenerateOneName(Country country) {
		return first_name_sampler.Sample() + ". " +
	        last_name_samplers.getOrDefault(country.name, first_name_sampler).Sample();
	}
	
	private WeightedSampler<String> first_name_sampler = new WeightedSampler<String>();
	private HashMap<String, WeightedSampler<String>> last_name_samplers =
			new HashMap<String, WeightedSampler<String>>();
}
