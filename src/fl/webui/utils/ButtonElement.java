package fl.webui.utils;

public class ButtonElement extends BaseElement {
	public ButtonElement(String text, String link) {
		this.text = text;
		this.link = link;
	}
	
	public ButtonElement(String text) {
		this.text = text;
	}
	
	@Override
	public String ToHtml() {
//		if (link == null) {
//			return "<button type='submit'>" + text + "</button>";
//		}
//		return "<button type='submit' formaction='" + link + "'>" + text + "</button>";
		if (link == null) {
			return "<input type='submit' value='" + text + "' />";
		}
		return "<input type='submit' formaction='" + link + "' value='" + text + "' />";
	}

	public String text;
	public String link;
}
