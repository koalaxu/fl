package fl.engine;

import fl.data.Event;
import fl.data.Global;
import fl.data.utils.FileUtil;
import fl.engine.data.DataAccessor;
import fl.engine.utils.Calendar;
import fl.utils.StringUtil;
import fl.webui.Server;

public class Engine {
	public Engine(DataAccessor data_accessor) {
		data_ = data_accessor;
		components_ = new ComponentHub(data_);
		controller_ = new ControllerModule(data_, components_);
	}
	
	public ControllerModule Controller() {
		return controller_;
	}
	
	public void Update() {
		Event event = calendar_.GetEvent(data_.GetGlobal().week, data_.GetGlobal().mid_week);
		if (event == null) return;
		switch (event.type) {
		case TRANSFER:
			components_.transfer.Preprocess(event.round);
		default:
			break;
		}
	}
	
	public void Proceed() {
		RoutineProcess();
		Event event = calendar_.GetEvent(data_.GetGlobal().week, data_.GetGlobal().mid_week);
		HandleEvent(event);
		AdvanceTime();
	}
	
	public void Done() {
		data_.Close();
	}
	
	private void RoutineProcess() {
		// Update Condition Data
		components_.condition_handler.Proceed();
		// Clear Match Commentary
		data_.ClearMatchCommentary();
	}
	
	private void AdvanceTime() {
		// Increment time
		Global global = data_.GetGlobal();
		if (!global.mid_week) {
			global.mid_week = true;
		} else {
			global.week++;
			global.mid_week = false;
		}
		// Next Year
		if (global.week == 52) {
			global.year++;
			global.week = 0;
			components_.player_processor.PassOneYear();
		}
	}
	
	private void HandleEvent(Event event) {
		System.out.println("Passing " + StringUtil.GetTime(data_.GetGlobal()) + " ...");
		if (event == null) return;
		switch (event.type) {
		case YOUTH_PROMOTION:
			components_.youth_generator.PromoteYouth();
			System.out.println("Youth promoted to clubs.");
			break;
		case MATCH_DRAW:
			components_.match_drawer.ArrangeMatches();
			System.out.println("Matches are arranged.");
			break;
		case CONTINENTAL_LEAGUE_KNOCKOUT_DRAW:
			components_.match_drawer.ArrangeKnockoutStage();
			System.out.println("Continental league knockout matches are arranged.");
			break;
		case TRANSFER:
			int num_transfer = components_.transfer.Process(event.round);
			System.out.println(num_transfer + " transfers finished.");
			break;
		case SQUAD_NUMBER_ASSIGNMENT:
			components_.squad_processor.Process();
			System.out.println("Squad number assigned.");
			components_.player_processor.RecordPlayerHistory();
			System.out.println("Player history recorded.");
			break;
		case MATCH:
			components_.league_processor.ProcessOneRound(event.game_type, event.round);
			break;
		case SEASON_END:
			components_.league_processor.End();
			break;
		case CONTRACT_RENEW:
			int retired = components_.contract_processor.RenewContract();
			System.out.println("Contracts renewed. " + retired + " players retired.");
			break;
		default:
			System.err.println("Unsupported Event.");
			System.exit(0);
			break;
		}
	}
	
	private DataAccessor data_;
	private Calendar calendar_ = new Calendar();
	private ComponentHub components_;
	private ControllerModule controller_;
	
	public static void main(String[] args) {
		FileUtil.Init();
		String dir = "test/";
		int rounds = 1;
		if (args.length > 0) {
			dir = args[0] + "/";
		}
		if (args.length > 1) {
			try {
				rounds = Integer.parseInt(args[1]);
			} catch (NumberFormatException e) {
				System.err.println("The second argument specifies the rounds to go.");
				System.exit(0);
			}
			Engine engine = new Engine(DataAccessor.CreateDataAccessor(dir));
			for (int i = 0; i < rounds; ++i) {
				engine.Proceed();
			}
			engine.Done();
		} else {
			Server.Start(dir);
		}
	}
}
