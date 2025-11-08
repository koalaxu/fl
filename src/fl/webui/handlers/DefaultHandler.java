package fl.webui.handlers;

import java.time.Duration;
import java.time.OffsetTime;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import fl.data.Event;
import fl.data.Global;
import fl.data.League;
import fl.engine.data.CountryData;
import fl.engine.data.DataAccessor;
import fl.engine.data.LeagueData;
import fl.utils.StringUtil;
import fl.webui.EngineRunner;
import fl.webui.utils.ButtonElement;
import fl.webui.utils.CompoundElement;
import fl.webui.utils.FormElement;
import fl.webui.utils.LinkedItem;
import fl.webui.utils.SplitElement;
import fl.webui.utils.TableElement;
import fl.webui.utils.TextElement;

public class DefaultHandler extends BaseHandler {
	@Override
	protected void HandleRequest(Map<String, String> parameters, Response response) {
		CompoundElement body = new CompoundElement();
		if (parameters.getOrDefault("command", "none").equals("proceed")) {
			if (lock.tryLock()) {
				try {
					if (!EngineRunner.running) {
						EngineRunner.running = true;
						DataAccessor.SetProgress(0);
						since_ = OffsetTime.now();
						EngineRunner runner = new EngineRunner(engine, lock);
						runner.start();
						BuildReturnPage(false, body);
					} else {
						BuildReturnPage(true, body);
					}
				} finally {
					lock.unlock();
				}
			} else {
				BuildReturnPage(true, body);
			}
			response.body = body.ToHtml();
			return;
		}
		if (lock.tryLock()) {
			try {
			  engine.Update();
			} finally {
				lock.unlock();
			}
		}
		
		Global global = accessor.GetGlobal();
		response.title = StringUtil.GetTime(global);
		TableElement table = new TableElement(3, false);
		for (CountryData country : accessor.GetAllCountries()) {
			table.AddElement(LinkedItem.CreateCountryItem(country));
			for (LeagueData league : accessor.GetAllLeauges()) {
				if (country.equals(league.GetCountry()) && league.GetData().type == League.Type.DOMESTIC_LEAGUE) {
					table.AddElement(LinkedItem.CreateLeagueItem(league));
				}
			}
		}
		for (LeagueData league : accessor.GetAllLeauges()) {
			if (league.GetCountry() == null) {
				table.AddElement(LinkedItem.CreateLeagueItem(league)).SetColumnSpan(3);
			}
		}
		body.AddElement(table);
		TextElement players = new TextElement("All Players");
		players.link = "/players";
		body.AddElement(players);
		body.AddElement(SplitElement.kSplitElement);
		TextElement transfers = new TextElement("All Transfers");
		transfers.link = "/transfers";
		body.AddElement(transfers);
		if (accessor.GetControlledClub() != null) {
			body.AddElement(SplitElement.kSplitElement);
			body.AddElement(new TextElement("Controlled Club: "));
			body.AddElement(LinkedItem.CreateClubItem(accessor.GetControlledClub()));
		}
		
		body.AddElement(SplitElement.kSplitElement);
		body.AddElement(new TextElement(GetEventMessage(calendar.GetEvent(global.week, global.mid_week))));
		String time_past = "n/a";
		if (since_ != null) {
		  Duration duration = Duration.between(since_, OffsetTime.now());
		  long second = duration.toMillis() / 1000;
		  time_past = String.format("%dm/%ds", second / 60, second % 60);
		}
		if (lock.tryLock()) {
			try {
				if (EngineRunner.running) {
					body.AddElement(new TextElement(
							String.format("Engine is processing... (%d%%) [%s]",
									DataAccessor.GetProgress(), time_past)));
				} else {
					FormElement form = new FormElement();
					form.AddElement(new ButtonElement("Proceed",
							ConstructLink("/home", parameters, "command", "proceed")));
					body.AddElement(form);
				}
			} finally {
				lock.unlock();
			}
		} else {
			body.AddElement(new TextElement(
					String.format("Engine is processing... (%d%%) [%s]",
							DataAccessor.GetProgress(), time_past)));
		}
		
		response.body = body.ToHtml();
	}
	
	private void BuildReturnPage(boolean already_running, CompoundElement body) {
		if (already_running) {
			body.AddElement(new TextElement("Engine is already running..."));
		} else {
			body.AddElement(new TextElement("Proceeding one week..."));
			Global global = accessor.GetGlobal();
			body.AddElement(new TextElement(GetEventMessage(calendar.GetEvent(global.week, global.mid_week))));
		}
		body.AddElement(SplitElement.kSplitElement);
		body.AddElement(new TextElement("Return").SetLink("/home"));
	}
	
	private String GetEventMessage(Event event) {
		if (event == null) {
			return "No event this week";
		}
		switch (event.type) {
		case YOUTH_PROMOTION:
			return "Promote youth players";
		case MATCH_DRAW:
			return "Arrange matches";
		case CONTINENTAL_LEAGUE_KNOCKOUT_DRAW:
			return "Arrange knockout matchs for continental leagues";
		case TRANSFER:
			return "Transfer";
		case SQUAD_NUMBER_ASSIGNMENT:
			return "Assign squad numbers";
		case MATCH:
			String str = "Match day: ";
			switch (event.game_type) {
			case CONTINENTAL_CUP:
				str += "Continental Cup";
				break;
			case CONTINENTAL_SUPER_CUP:
				str += "Continental Super Cup";
				break;
			case DOMESTIC_CUP:
				str += "Domstic Cup";
				break;
			case DOMESTIC_LEAGUE:
				str += "Domestic League";
				break;
			case DOMESTIC_SUPER_CUP:
				str += "Domestic Super Cup";
				break;
			}
			if (event.round != null) {
				str += " - Round " + (event.round + 1);
			}
			return str;
		case SEASON_END:
			return "The end of this season";
		case CONTRACT_RENEW:
			return "Renew contracts";
		case NONE:
			return "No event this week";
		}
		return null;
	}
	
	private Lock lock = new ReentrantLock();
	private static OffsetTime since_;
}
