package fl.webui.utils;

import java.util.ArrayList;
import java.util.List;

public class CompoundElement extends BaseElement {
	public CompoundElement() {
		splitter = "<br>";
	}
	public CompoundElement(String splitter) {
		this.splitter = splitter;
	}
	public void AddElement(BaseElement new_element) {
		elements.add(new_element);
	}
	
	@Override
	public String ToHtml() {
		String str = "";
		for (int i = 0; i < elements.size(); ++i) {
			str += (i > 0 ? splitter : "") + elements.get(i).ToHtml();
		}
		return str;
	}

	private List<BaseElement> elements = new ArrayList<BaseElement>();
	private String splitter;
}
