package fl.webui.utils;

import java.util.ArrayList;
import java.util.List;

import fl.webui.utils.TableElement.Cell.Alignment;

public class TableElement extends BaseElement {
	public TableElement(int num_column, boolean bold_title) {
		this.num_column = num_column;
		this.bold_title = bold_title;
	}
	
	public static class Cell {
		public Cell(BaseElement element) {
			this.element = element;
		}
		public enum Alignment {
			LEFT,
			CENTER,
			RIGHT,
		}
		public Cell SetColumnSpan(int colspan) {
			this.colspan = colspan;
			return this;
		}
		public Cell SetAlignment(Alignment alignment) {
			this.alignment = alignment;
			return this;
		}
		public BaseElement element;
		public int colspan = 1;
		public Alignment alignment = Alignment.LEFT;
	}
	
	public void SetDefaultAlignment(Alignment alignment) {
		default_alignment = alignment;
	}
	
	public Cell AddElement(BaseElement new_element) {
		Cell cell = new Cell(new_element);
		cell.alignment = default_alignment;
		cells.add(cell);
		return cell;
	}
	

	@Override
	public String ToHtml() {
		String str = "<table style='width:100%'>";
		int index = 0;
		for (Cell cell : cells) {
			
			if (index % num_column == 0) str += "<tr>";
			if (bold_title && index < num_column) {
				str += GetTdHead("th", cell) + cell.element.ToHtml() + "</th>";
			} else {
				str += GetTdHead("td", cell) + cell.element.ToHtml() + "</td>";
			}
			index += cell.colspan;
			if (index % num_column == 0) str += "</tr>";
		}
		str += "</table>";
		return str;
	}
	
	private static String GetTdHead(String tag, Cell cell) {
		String str = "<" + tag;
		if (cell.colspan > 1) str += " colspan='" + cell.colspan + "'";
		if (cell.alignment != Alignment.LEFT) {
			str += " style='text-align:" + cell.alignment.toString().toLowerCase() + "'";
		}
		return str + ">";
	}

	private int num_column;
	private boolean bold_title;
	private List<Cell> cells = new ArrayList<Cell>();
	private Cell.Alignment default_alignment = Alignment.LEFT;
}
