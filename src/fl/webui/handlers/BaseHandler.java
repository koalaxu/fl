package fl.webui.handlers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import fl.engine.Engine;
import fl.engine.data.ClubData;
import fl.engine.data.CountryData;
import fl.engine.data.DataAccessor;
import fl.engine.data.LeagueData;
import fl.engine.utils.Calendar;
import fl.webui.utils.CompoundElement;
import fl.webui.utils.LinkedItem;
import fl.webui.utils.TextElement;

public abstract class BaseHandler implements HttpHandler {
	public static void Init(Engine engine, DataAccessor accessor) {
		BaseHandler.engine = engine;
		BaseHandler.accessor = accessor;
	}
	
	protected static class Response {
		public String title;
		public String body;
		public String script;
		public CountryData country;
		public LeagueData league;
		public ClubData club;
		public int year;
	}
	
	protected abstract void HandleRequest(Map<String, String> parameters, Response response);
	// Return true if year menu is needed.
	protected boolean GetMenu(List<String> menu_list) {  return false;  }
	
	@Override
	public void handle(HttpExchange exchange) throws IOException {
		try {
			String html = kHtmlStart;
			List<String> menu_list = new ArrayList<String>();
			boolean build_year_menu = GetMenu(menu_list);
			Map<String, String> params = ExtractParameters(exchange);
			
			Response response = new Response();
			HandleRequest(params, response);
			if (response.script != null && !response.script.isEmpty()) {
			  html = html + kScriptStart + response.script + kScriptEnd;
			}
			html = html + kHeaderEnd;
			if (response.title != null) {
				html = html + BuildTitle(response.title);
			}
			if (!menu_list.isEmpty() || build_year_menu) {
				html = html + BuildMenu(response, menu_list, params,
						exchange.getRequestURI().getPath(), build_year_menu);
			}
			html = html + response.body + kHtmlEnd;
			byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(200, bytes.length);
			OutputStream os = exchange.getResponseBody();
			os.write(bytes);
			os.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	protected String ConstructLink(String path, Map<String, String> params, String new_key, String new_value) {
		String query = "?";
		Map<String, String> new_params = new HashMap<String, String>(params);
		new_params.put(new_key, new_value);
		for (Entry<String, String> param : new_params.entrySet()) {
			query = query + (query.length() > 1 ? "&" : "") + param.getKey() + "=" + param.getValue();
		}
		return path + query;
	}
	
	protected String BuildAlert(String msg) {
		return "window.onload=function(){alert('" + msg + "');}";
	}
	
	private Map<String, String> ExtractParameters(HttpExchange exchange) {
		URI uri = exchange.getRequestURI();
		Map<String, String> ret = new HashMap<String, String>();
		String query_str = uri.getQuery();
		AddParameters(ret, query_str);
		try {
			InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
			BufferedReader br = new BufferedReader(isr);
			AddParameters(ret, br.readLine());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return ret;
	}
	
	private void AddParameters(Map<String, String> params, String query_str) {
		if (query_str != null) {
			String[] queries = query_str.split("&");
			if (queries != null) {
				for (String query : queries) {
					String[] kv = query.split("=", 2);
					params.put(kv[0], kv[1]);
				}
			}
		}
	}
	
	private String BuildMenu(Response response, List<String> menu_list, Map<String, String> params, String path,
			boolean build_year_menu) {
		CompoundElement menu = new CompoundElement(" | ");
		String query = "?";
		String current_tab = params.get("tab");
		for (Entry<String, String> param : params.entrySet()) {
			if (param.getKey().equals("tab") || param.getKey().equals("command")) continue;
			query = query + (query.length() > 1 ? "&" : "") + param.getKey() + "=" + param.getValue();
		}

		for (int i = 0; i < menu_list.size(); ++i) {
			TextElement button = new TextElement(menu_list.get(i));
			String actual_query = query + (i > 0 ? (query.length() > 1 ? "&" : "") +
					"tab=" + menu_list.get(i).toLowerCase() : "");
			button.link = path + (actual_query.length() > 1 ? actual_query : "");
			if ((current_tab == null && i == 0) || menu_list.get(i).toLowerCase().equals(current_tab)) {
				button.bold = true;
			}
			menu.AddElement(button);
		}
		String year_menu_text = "";
		if (build_year_menu) {
			CompoundElement year_menu = new CompoundElement(" | ");
			int current_year = accessor.GetGlobal().year;
			int year = Integer.valueOf(params.getOrDefault("year", String.valueOf(current_year)));
			TextElement prev = new TextElement(year - 1);
			prev.link = ConstructLink(path, params, "year", String.valueOf(year - 1));
			year_menu.AddElement(prev);
			year_menu.AddElement(new TextElement(year).SetBold(true));
			TextElement next = new TextElement(year + 1);
			if (year < current_year) {
				next.link = ConstructLink(path, params, "year", String.valueOf(year + 1));
			}
			year_menu.AddElement(next);
			year_menu_text = " <br> < " + year_menu.ToHtml() + " > ";
		}
		
		CompoundElement navigation_menu = new CompoundElement(" > ");
		navigation_menu.AddElement(new TextElement("Home").SetLink("/home"));
		if (response.country != null) {
			navigation_menu.AddElement(LinkedItem.CreateCountryItem(response.country));
		}
		if (response.league != null) {
			if (response.year > 0) {
				navigation_menu.AddElement(LinkedItem.CreateLeagueItem(response.league, response.year));
			} else {
				navigation_menu.AddElement(LinkedItem.CreateLeagueItem(response.league));
			}
		}
		if (response.club != null) {
			navigation_menu.AddElement(LinkedItem.CreateClubItem(response.club));
		}
		
		return  "[" + navigation_menu.ToHtml() + "] <br> " + menu.ToHtml() + year_menu_text + "<br><br>";
	}
	
	private String BuildTitle(String str) {
		TextElement title = new TextElement(str);
		title.title = true;
		return title.ToHtml();
	}

	protected static DataAccessor accessor;
	protected static Calendar calendar = new Calendar();
	protected static Engine engine;
	
	private static String kHtmlStart = "<html><meta charset=\"UTF-8\"><head><style>" +
			"table, th, td { border: 1px solid gray } " +
			"th, td { padding: 15px } " +
			"th { font-weight: bold; font-size: 36px } " +
			"td { font-size: 36px } " +
			"body { font-size: 36px} " +
			"p { font-size: 36px } " +
			"h1 { font-weight: bold; font-size: 40px } " +
			"input { font-size: 36px } " +
			"select { font-size: 36px } " +
			"button { font-size: 36px } " +
			"</style>";
	private static String kScriptStart = "<script type='text/javascript'>";
	private static String kScriptEnd = "</script>";
	private static String kHeaderEnd = "</head><body>";
	private static String kHtmlEnd = "</body></html>";
}
