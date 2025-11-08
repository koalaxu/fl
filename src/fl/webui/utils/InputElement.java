package fl.webui.utils;

public class InputElement extends BaseElement {
	public InputElement(String name, Long content) {
		this.name = name;
		if (content != null) this.content = content.toString();
		else this.content = "";
	}
	public InputElement(String name, Integer content) {
		this.name = name;
		if (content != null) this.content = content.toString();
		else this.content = "";
	}
	@Override
	public String ToHtml() {
		return "<input type='text' name='" + name + "' value='" + content + "' />";
	}

	private String name;
	private String content;
}
