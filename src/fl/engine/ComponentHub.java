package fl.engine;

import fl.engine.data.ClubData;
import fl.engine.data.DataAccessor;
import fl.engine.data.PlayerData;
import fl.engine.utils.PriceModel;
import fl.engine.utils.WageModel;

public class ComponentHub {
	public ComponentHub(DataAccessor data_accessor) {
		youth_generator = new YouthGenerator(data_accessor, this);
		match_drawer = new MatchDrawer(data_accessor, this);
		transfer = new TransferProcessor(data_accessor, this);
		income_calculator = new IncomeCalculator(data_accessor, this);
		cost_calculator = new CostCalculator(data_accessor, this);
		squad_processor = new SquadProcessor(data_accessor, this);
		league_processor = new LeagueProcessor(data_accessor, this);
		match_simulator = new MatchSimulator(data_accessor, this);
		condition_handler = new ConditionHandler(data_accessor, this);
		contract_processor = new ContractProcessor(data_accessor, this);
		player_processor = new PlayerProcessor(data_accessor, this);
		price_model = new PriceModel(data_accessor.GetGlobal());
	}
	public YouthGenerator youth_generator;
	public MatchDrawer match_drawer;
	public TransferProcessor transfer;
	public IncomeCalculator income_calculator;
	public CostCalculator cost_calculator;
	public SquadProcessor squad_processor;
	public LeagueProcessor league_processor;
	public MatchSimulator match_simulator;
	public ConditionHandler condition_handler;
	public ContractProcessor contract_processor;
	public PlayerProcessor player_processor;
	
	
	public void RefreshWageModel(DataAccessor data_accessor) {
		wage_model = new WageModel();
		for (ClubData club : data_accessor.GetAllClubs()) {
			for (PlayerData player : club.GetPlayers()) {
				if (player.GetInfo().club_info.wage != null) {
					wage_model.AddOnePlayerToModel(player.GetInfo());
				}
			}
		}
		wage_model.Learn();
	}
	
	public PriceModel price_model;
	public WageModel wage_model = new WageModel();
}
