package fl.webui.utils;

import java.util.Map;
import java.util.Map.Entry;

public class SelectElement extends BaseElement {
	public SelectElement(String name, Map<String, String> values) {
		this.name = name;
		this.values = values;
	}
	public void SetDefault(String default_option) {
		this.default_option = default_option;
	}
	@Override
	public String ToHtml() {
		String output = "<select name='" + name + "'>";
		for (Entry<String, String> kv : values.entrySet()) {
			output = output + "<option value='" + kv.getValue() + "'"
					+ (kv.getValue().equals(default_option) ? " selected>" : ">")
		            + kv.getKey() + "</option>";
		}
		output = output + "</select>";
		return output;
	}

	private String name;
	private Map<String, String> values;
	private String default_option;
}
