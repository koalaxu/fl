package fl.webui.utils;

public class SplitElement extends BaseElement {

	private SplitElement() {}
	
	@Override
	public String ToHtml() {
		return "<br>";
	}

	public static SplitElement kSplitElement = new SplitElement();
}
