package fl.webui.utils;

import org.apache.commons.text.StringEscapeUtils;

public class TextElement extends BaseElement  {
	public TextElement(String content) {
		if (content != null) this.content = content;
		else this.content = "";
	}
	
	public TextElement(Integer content) {
		if (content != null) this.content = content.toString();
		else this.content = "";
	}
	
	public TextElement(Long content) {
		if (content != null) this.content = content.toString();
		else this.content = "";
	}
	
	public TextElement SetLink(String link) {
		this.link = link;
		return this;
	}
	
	public TextElement SetTitle(boolean title) {
		this.title = title;
		return this;
	}
	
	public TextElement SetBold(boolean bold) {
		this.bold = bold;
		return this;
	}
	
	@Override
	public String ToHtml() {
		String str = title ? ("<h1>" + GetContent() + "</h1>") :
			bold ? ("<b>" + GetContent() + "</b>") : ("" + GetContent() + "");
		if (link == null) return str;
		return "<a href='" + link + "'>" + str + "</a>";
	}
	
	private String GetContent() {
		return StringEscapeUtils.escapeHtml4(content).replaceAll("\n", "<br>");
	}
	
	public String content;
	public String link;
	public boolean title;
	public boolean bold;
}
