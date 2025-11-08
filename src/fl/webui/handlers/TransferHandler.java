package fl.webui.handlers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fl.data.PlayerHistory.PlayerRecord;
import fl.data.Position;
import fl.engine.data.PlayerData;
import fl.engine.data.TransferData;
import fl.engine.utils.PositionAnalyzer;
import fl.utils.StringUtil;
import fl.webui.utils.CompoundElement;
import fl.webui.utils.LinkedItem;
import fl.webui.utils.TableElement;
import fl.webui.utils.TextElement;

public class TransferHandler extends BaseHandler {
	public TransferHandler() {
		if (kAbbreviateToPositions == null) {
			kAbbreviateToPositions = new HashMap<String, Position>();
			for (Position pos : Position.positions.values()) {
				kAbbreviateToPositions.put(pos.abbreviate, pos);
			}
		}
	}

	@Override
	protected boolean GetMenu(List<String> menu_list) {
		for (Position pos : Position.positions.values()) {
			menu_list.add(pos.abbreviate);
		}
		return true;
	}

	@Override
	protected void HandleRequest(Map<String, String> parameters, Response response) {
		int year = Integer.valueOf(parameters.getOrDefault("year",
				Integer.valueOf(accessor.GetGlobal().year).toString()));
		int years_ago = (int) (accessor.GetGlobal().year - year);
		String tab = parameters.getOrDefault("tab", "gk").toUpperCase();
		response.title = "All Transfers ( " + year + " - " + tab + " )";
		CompoundElement body = new CompoundElement();
		
		Position position = kAbbreviateToPositions.getOrDefault(tab, Position.GoalKeeper);
		List<TransferData> transfers = new ArrayList<TransferData>();
		for (TransferData transfer: accessor.GetAllTransfers(year)) {
			if (transfer.GetPlayer().GetPosition().equals(position)) {
				transfers.add(transfer);
			}
		}
		transfers.sort(new DescendingTransferFee());
		TableElement table = new TableElement(6, true);
		for (String header : kHeaders) table.AddElement(new TextElement(header));
		for (TransferData transfer : transfers) {
			PlayerData player = transfer.GetPlayer();
			table.AddElement(LinkedItem.CreatePlayerItem(player));
			table.AddElement(new TextElement(player.GetAge() - years_ago));
			PlayerRecord record = player.GetAnnualRecord(year);
			table.AddElement(new TextElement((int)((record != null ? record.score :
				PositionAnalyzer.GetOverallScore(player.GetInfo())) * 100)));
			table.AddElement(LinkedItem.CreateClubItem(transfer.GetFromClub()));
			table.AddElement(LinkedItem.CreateClubItem(transfer.GetToClub()));
			table.AddElement(new TextElement(StringUtil.ShortNumber(transfer.GetFee())));
		}
		body.AddElement(table);
		response.body = body.ToHtml();
	}
	
	private static class DescendingTransferFee implements Comparator<TransferData> {

		@Override
		public int compare(TransferData o1, TransferData o2) {
			if (o1.GetFee() > o2.GetFee()) return -1;
			if (o1.GetFee() < o2.GetFee()) return 1;
			return 0;
		}
		
	}

	private static Map<String, Position> kAbbreviateToPositions;
	private static String[] kHeaders = { "Name", "Age", "Ability", "From", "To", "Fee"};
}
