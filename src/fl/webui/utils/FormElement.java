package fl.webui.utils;

public class FormElement extends CompoundElement {
	public FormElement() {
	}
	public FormElement(String action) {
		this.action = action;
	}
	@Override
	public String ToHtml() {
		if (action == null) {
			return "<form method='POST'>" + super.ToHtml() + "</form>";
		}
		return "<form action='" + action + "' method='POST' >" + super.ToHtml() + "</form>";
	}
	
	private String action;
}
