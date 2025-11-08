package fl.webui;

import java.io.IOException;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpServer;

import fl.data.utils.FileUtil;
import fl.engine.Engine;
import fl.engine.data.DataAccessor;
import fl.webui.handlers.BaseHandler;
import fl.webui.handlers.ClubHandler;
import fl.webui.handlers.CountryHandler;
import fl.webui.handlers.DefaultHandler;
import fl.webui.handlers.LeagueHandler;
import fl.webui.handlers.MatchHandler;
import fl.webui.handlers.PlayerHandler;
import fl.webui.handlers.PlayersHandler;
import fl.webui.handlers.TransferHandler;

public class Server {
	public static void Start(String data_sub_dir) {
		DataAccessor accessor = DataAccessor.CreateDataAccessor(data_sub_dir);
		engine = new Engine(accessor);
		BaseHandler.Init(engine, accessor);
		try {
			server = HttpServer.create(new InetSocketAddress(8080), 0);
			server.createContext("/home", new DefaultHandler());
			server.createContext("/league", new LeagueHandler());
			server.createContext("/country", new CountryHandler());
			server.createContext("/club", new ClubHandler());
			server.createContext("/player", new PlayerHandler());
			server.createContext("/match", new MatchHandler());
			server.createContext("/players", new PlayersHandler());
			server.createContext("/transfers", new TransferHandler());
	        server.setExecutor(null); // creates a default executor
	        server.start();
			System.err.println("Ready");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	private static HttpServer server;
	
	public static void main(String[] args) {
		FileUtil.Init();
		String dir = "test/";
		if (args.length > 0) {
			dir = args[0] + "/";
			
		}
		Server.Start(dir);
	}
	
	private static Engine engine = null;
}
