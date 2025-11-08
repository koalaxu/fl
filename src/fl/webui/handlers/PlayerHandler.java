package fl.webui.handlers;

import java.util.List
;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;

import fl.data.Ability;
import fl.data.Global;
import fl.data.Ability.Type;
import fl.data.Event;
import fl.data.Event.EventType;
import fl.data.PlayerHistory.InjuryRecord;
import fl.data.PlayerHistory.PlayerRecord;
import fl.data.PlayerInfo;
import fl.data.PlayerLeagueRecord;
import fl.data.Position;
import fl.engine.ControllerModule.BidStatus;
import fl.engine.ControllerModule.RenewalStatus;
import fl.engine.data.ClubData;
import fl.engine.data.LeagueData;
import fl.engine.data.MarketData;
import fl.engine.data.PlayerData;
import fl.engine.data.PlayerLeagueData;
import fl.engine.data.TransferData;
import fl.engine.data.MarketData.Supply;
import fl.engine.utils.MatchStatsUtil;
import fl.engine.utils.PositionAnalyzer;
import fl.utils.FieldAccessor;
import fl.utils.StringUtil;
import fl.webui.utils.BaseElement;
import fl.webui.utils.ButtonElement;
import fl.webui.utils.CompoundElement;
import fl.webui.utils.FormElement;
import fl.webui.utils.InputElement;
import fl.webui.utils.LinkedItem;
import fl.webui.utils.SplitElement;
import fl.webui.utils.TableElement;
import fl.webui.utils.TextElement;
import fl.webui.utils.TableElement.Cell.Alignment;

public class PlayerHandler extends BaseHandler {
	@Override
	protected boolean GetMenu(List<String> menu_list) {
		menu_list.add("Info");
		menu_list.add("History");
		menu_list.add("HistoryStats");
		menu_list.add("HistoryLeagueStats");
		return false;
	}

	@Override
	protected void HandleRequest(Map<String, String> parameters, Response response) {
		long id = Long.valueOf(parameters.get("id"));	
		String command = parameters.getOrDefault("command", "none");
		long fee = (long) (Float.valueOf(parameters.getOrDefault("fee", "0")) * 1000000);
		int wage = (int) (Float.valueOf(parameters.getOrDefault("wage", "0")) * 1000000);		
		if (command.equals("update_fee")) {
		    engine.Controller().OverrideMinTransferFee(id, fee);
		} else if (command.equals("bid")) {
			BidStatus bid_status = engine.Controller().Bid(id, fee, wage);
			switch (bid_status) {
			case ACCEPTED:
				response.script = this.BuildAlert("Transfer success");
				accessor.Flush();
				break;
			case NO_SUFFICIENT_FEE:
				response.script = this.BuildAlert("No sufficient fund");
				break;
			case REJECTED_TRANSFER_FEE:
				response.script = this.BuildAlert("Transfer fee is low");
				break;
			case REJECTED_WAGE:
				response.script = this.BuildAlert("Wage is low");
				break;
			}
		} else if (command.equals("renew")) {
			int contract_length = Integer.valueOf(parameters.getOrDefault("contract_length", "0"));
			RenewalStatus renewal_status = engine.Controller().RenewContract(id, wage, contract_length);
			switch (renewal_status) {
			case ACCEPTED:
				response.script = this.BuildAlert("Renewal success");
				accessor.Flush();
				break;
			case REJECTED_WAGE:
				response.script = this.BuildAlert("Wage is low");
				break;
			case REJECTED_LENGTH:
				response.script = this.BuildAlert("Contract length is long");
				break;
			}
		}
		String tab = parameters.getOrDefault("tab", "default");
		PlayerData player = accessor.GetPlayer(id);
		CompoundElement body = new CompoundElement();
		Position pos = player.GetPosition();
		response.title = player.GetData().name + " ( Age: " + player.GetAge() + ") " +
				(pos != null ? " - " + pos.abbreviate : "");

		if (tab.equals("default")) {
			BuildInfo(player, body, parameters);
		} else if (tab.equals("history")) {
			BuildHistory(player, body);
		} else if (tab.equals("historystats")) {
			BuildHistoryStats(player, body, false);
		} else if (tab.equals("historyleaguestats")) {
			BuildHistoryStats(player, body, true);
		}
		response.body = body.ToHtml();
		ClubData club = player.GetClub();
		if (club != null) {
			response.club = club;
			response.league = club.FindDomesticLeague();
			response.country = club.GetCountry();
		}
	}
	
	private void BuildInfo(PlayerData player, CompoundElement body, Map<String, String> parameters) {
		PlayerInfo info = player.GetInfo();
		if (info == null) {
			body.AddElement(new TextElement("Retired").SetBold(true));
			return;
		}
		TableElement table = new TableElement(4, true);
		table.SetDefaultAlignment(Alignment.CENTER);
		for (String head : kHeaders) {
			table.AddElement(new TextElement(head));
		}
		table.AddElement(new TextElement(info.GetPosition().abbreviate + String.format(" (%d)", (int)(
				PositionAnalyzer.GetOverallScore(info, info.GetPosition()) * 100))));
		if (info.secondary_position != null) {
			table.AddElement(new TextElement(info.GetSecondaryPosition().abbreviate + String.format(" (%d)", (int)(
					PositionAnalyzer.GetOverallScore(info, info.GetSecondaryPosition()) * 100))));
		} else {
			table.AddElement(new TextElement("/"));
		}
		table.AddElement(new TextElement(kFootProficient[FieldAccessor.kZeroBased.Get(info.left_foot)]));
		table.AddElement(new TextElement(kFootProficient[FieldAccessor.kZeroBased.Get(info.right_foot)]));
		
		for (String header : kConditionHeader) {
			TextElement text = new TextElement(header);
			text.bold = true;
			table.AddElement(text);
		}
		table.AddElement(new TextElement(FieldAccessor.kHundredBased.Get(info.stamina)));
		table.AddElement(new TextElement(FieldAccessor.kHundredBased.Get(info.condition)));
		table.AddElement(new TextElement(StringUtil.ShortTime(FieldAccessor.kZeroBased.Get(info.injury))));
		table.AddElement(new TextElement(FieldAccessor.kZeroBased.Get(info.exp)));
		body.AddElement(table);
		
		table = new TableElement(5, true);
		table.SetDefaultAlignment(Alignment.CENTER);
		for (int i = 0; i < 5; ++i) {
			table.AddElement(new TextElement(kAbilityHeader0[i]));
		}
		for (int i = 0; i < 5; ++i) {
			table.AddElement(new TextElement(info.ability.GetAbility(kAbility0[i])));
		}
		for (int i = 0; i < 5; ++i) {
			TextElement text = new TextElement(kAbilityHeader1[i]);
			text.bold = true;
			table.AddElement(text);
		}
		for (int i = 0; i < 5; ++i) {
			table.AddElement(new TextElement(info.ability.GetAbility(kAbility1[i])));
		}
		body.AddElement(table);
		
		if (info.club_info != null) {
			table = new TableElement(4, true);
			table.SetDefaultAlignment(Alignment.CENTER);
			for (String head : kClubHeader) {
				table.AddElement(new TextElement(head));
			}
			table.AddElement(LinkedItem.CreateClubItem(accessor.GetClub(info.club_id)));
			table.AddElement(new TextElement(info.club_info.squad_number));
			table.AddElement(new TextElement(StringUtil.ShortNumber(info.club_info.wage)));
			table.AddElement(new TextElement(info.club_info.contract + " yr"));
		}
		body.AddElement(table);
		Global global = accessor.GetGlobal();
		Event event = calendar.GetEvent(global.week, global.mid_week);
		if (event == null) return;
		if (event.type == EventType.TRANSFER) {
			BuildTransferSection(player, body, parameters);
		} else if (event.type == EventType.CONTRACT_RENEW) {
			BuildContractSection(player, body, parameters);
		}
	}
	
	private void BuildTransferSection(PlayerData player, CompoundElement body, Map<String, String> parameters) {
		MarketData market_data = accessor.GetMarketData();
		if (market_data == null) return;
		Supply supply = market_data.FindSupply(player);
		if (supply == null) return;
		if (player.GetClub().equals(accessor.GetControlledClub())) {
			FormElement form = new FormElement();
			form.AddElement(new TextElement("Min Transfer Fee (M): "));
			form.AddElement(new InputElement("fee", supply.min_transfer_fee / 1000000));
			form.AddElement(new ButtonElement("Update",
					ConstructLink("/player", parameters, "command", "update_fee")));
			body.AddElement(form);
		} else {
			FormElement form = new FormElement();
			form.AddElement(new TextElement("Transfer Fee (M): "));
			form.AddElement(new InputElement("fee", 0L));
			form.AddElement(new TextElement("Wage (M): "));
			form.AddElement(new InputElement("wage", 0L));			
			form.AddElement(new ButtonElement("Bid",
					ConstructLink("/player", parameters, "command", "bid")));
			body.AddElement(form);			
		}
	}
	
	private void BuildContractSection(PlayerData player, CompoundElement body, Map<String, String> parameters) {
		if (!player.GetClub().equals(accessor.GetControlledClub())) return;
		FormElement form = new FormElement();
		form.AddElement(new TextElement("New Wage (M): "));
		form.AddElement(new InputElement("wage", 0L));
		form.AddElement(new TextElement("Extract Contract Length: "));
		form.AddElement(new InputElement("contract_length", 1));			
		form.AddElement(new ButtonElement("Renew",
				ConstructLink("/player", parameters, "command", "renew")));
		body.AddElement(form);	
	}
	
	private void BuildHistory(PlayerData player, CompoundElement body) {
		TableElement table = new TableElement(5, true);
		for (String header : kHistoryHeader) {
			table.AddElement(new TextElement(header));
		}
		SortedMap<Integer, PlayerRecord> records = player.GetAnnualRecords();
		if (records != null) {
			for (Entry<Integer, PlayerRecord> record : records.entrySet()) {
				table.AddElement(new TextElement(record.getKey()));
				table.AddElement(LinkedItem.CreateClubItem(accessor.GetClub(record.getValue().club_id)));
				table.AddElement(new TextElement(record.getValue().squad_number));
				table.AddElement(new TextElement(StringUtil.ShortNumber(record.getValue().wage)));
				table.AddElement(new TextElement(String.format("%d", (int)(record.getValue().score * 100))));
			}
		}
		body.AddElement(table);
		body.AddElement(SplitElement.kSplitElement);
		
		table = new TableElement(4, true);
		for (String header : kTransferHeader) {
			table.AddElement(new TextElement(header));
		}
		for (TransferData transfer : player.GetTransfers()) {
			table.AddElement(new TextElement(transfer.GetYear()));
			table.AddElement(LinkedItem.CreateClubItem(transfer.GetFromClub()));
			table.AddElement(LinkedItem.CreateClubItem(transfer.GetToClub()));
			table.AddElement(new TextElement(StringUtil.ShortNumber(transfer.GetFee())));
		}
		body.AddElement(table);
		body.AddElement(SplitElement.kSplitElement);
		
		table = new TableElement(2, true);
		table.AddElement(new TextElement("Injury Time"));
		table.AddElement(new TextElement("Injury Length"));
		List<InjuryRecord> injury_history = player.GetInjuryRecord();
		if (injury_history != null) {
			for (InjuryRecord injury_record : injury_history) {
				table.AddElement(new TextElement(injury_record.year + " - Week # " + injury_record.week));
				table.AddElement(new TextElement(StringUtil.ShortTime(injury_record.length)));
			}
		}
		body.AddElement(table);
	}
	
	private void BuildHistoryStats(PlayerData player, CompoundElement body, boolean show_league) {
		SortedMap<Integer, PlayerRecord> records = player.GetAnnualRecords();
		if (records == null) return;
		TableElement table = new TableElement(21, true);
		PlayerLeagueRecord career_total = new PlayerLeagueRecord();
		AddHeaderToTable("League", table);
		for (Entry<Integer, PlayerRecord> record : records.entrySet()) {
			int year = record.getKey();
			ClubData club = accessor.GetClub(record.getValue().club_id);
			PlayerLeagueRecord year_total = new PlayerLeagueRecord();
			for (LeagueData league : accessor.GetAllLeauges()) {
				if (league.GetCountry() != null && !club.GetCountry().equals(league.GetCountry())) continue;
				PlayerLeagueData info = player.GetLeagueInfo(league);
				PlayerLeagueRecord stats = info.GetLeagueRecord(year);
				if (stats == null) continue;
				if (show_league) AddStatsToTable(LinkedItem.CreateLeagueItem(league), stats, table);
				MatchStatsUtil.RollUpPlayerStats(stats, year_total);
			}
			if (year_total.match_played > 0) {
				AddStatsToTable(new TextElement(year).SetBold(true), year_total, table);
				MatchStatsUtil.RollUpPlayerStats(year_total, career_total);
				if (show_league) AddHeaderToTable("", table);
			}
		}
		if (career_total.match_played > 0) {
			AddStatsToTable(new TextElement("Total").SetBold(true), career_total, table);
		}
		body.AddElement(table);
		body.AddElement(SplitElement.kSplitElement);
	}
	
	public static void AddHeaderToTable(String row_title, TableElement table) {
		table.AddElement(new TextElement(row_title).SetBold(true));
		for (String head : kStatsHeaders) table.AddElement(new TextElement(head).SetBold(true));
	}
	
	public static void AddStatsToTable(BaseElement row_title, PlayerLeagueRecord stats, TableElement table) {
		table.AddElement(row_title);
		if (stats.match_played == 0) {
			final TextElement empty = new TextElement("");
			for (int i = 0; i < 20; ++i) table.AddElement(empty);
			return;
		}
		table.AddElement(new TextElement(stats.match_played + "(" + stats.lineup_match_playered + ")"));
		table.AddElement(new TextElement((int)stats.time_played / 60 / stats.match_played));
		table.AddElement(new TextElement(stats.stats.goal));
		table.AddElement(new TextElement(stats.stats.assistance));
		table.AddElement(new TextElement(stats.stats.goal_conceded));
		table.AddElement(new TextElement(stats.stats.yellow));
		table.AddElement(new TextElement(stats.stats.red));
		table.AddElement(new TextElement(String.format("%.1f(%d)",
				(float)(stats.average_score / 10) / stats.match_played,  stats.mom)));
		table.AddElement(MakeAverage(stats.stats.key_pass, stats.match_played));
		table.AddElement(MakeRatio(stats.stats.pass_succeed, stats.stats.pass, stats.match_played));
		table.AddElement(MakeRatio(
				stats.stats.dribble_succeed, stats.stats.dribble, stats.match_played));
		table.AddElement(MakeAverage(stats.stats.shot_ontarget, stats.match_played));
		table.AddElement(MakeAverage(stats.stats.shot, stats.match_played));
		table.AddElement(MakeRatio(stats.stats.header_succeed, stats.stats.header, stats.match_played));
		table.AddElement(MakeAverage(stats.stats.clearance, stats.match_played));
		table.AddElement(MakeAverage(stats.stats.intecept, stats.match_played));
		table.AddElement(MakeAverage(stats.stats.tackle, stats.match_played));
		table.AddElement(MakeAverage(stats.stats.save, stats.match_played));
		table.AddElement(MakeAverage(stats.stats.fouled, stats.match_played));
		table.AddElement(MakeAverage(stats.stats.foul, stats.match_played));
	}
	
	private static TextElement MakeAverage(int value, int matches) {
		return new TextElement(String.format("%.1f", (double)value / matches));
	}
	
	private static TextElement MakeRatio(int numerator, int denominator, int matches) {
		if (denominator == 0) return new TextElement("0");
		return new TextElement(String.format("%.1f", (double)denominator / matches)
				+ " (" + numerator * 100 / denominator + "%)");
	}

	private static String[] kHeaders = { "Position", "2nd Position", "Left Foot", "Right Foot" };
	private static String[] kAbilityHeader0 = { "Pace", "Agility", "Strength", "Jump", "Goalkeeping" };
	private static String[] kAbilityHeader1 = { "Dribble", "Pass", "Shot", "Tackle", "Position" };
	private static String[] kConditionHeader = { "Stamina", "Condition", "Injury", "Exp" };
	private static String[] kClubHeader = { "Club", "Squad No.", "Wage", "Contract" };
	private static String[] kHistoryHeader = { "Year", "Club", "Squad #", "Wage", "Ability" };
	private static String[] kTransferHeader = { "Year", "From", "To", "Price" };
	private static Ability.Type[] kAbility0 = { Type.PACE, Type.AGILITY, Type.STRENGTH, Type.JUMP, Type.GOALKEEP };
	private static Ability.Type[] kAbility1 = { Type.DRIBBLE, Type.PASS, Type.SHOT, Type.TACKLE, Type.POSITION };
	private static String[] kFootProficient = { "/", "awkward", "ok", "natural" };
	
	private static String[] kStatsHeaders = { "M(S)", "Min", "G", "A", "C", "Y", "R", "Sc(MoM)",
			"KP", "P(%)", "D(%)", "SO", "Sh", "H(%)", "C", "I", "T", "Sv", "Fd", "F" };
}
