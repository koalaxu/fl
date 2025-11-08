package fl.engine;

import java.util.ArrayList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fl.data.Constant;
import fl.data.PlayerInfo;
import fl.engine.data.ClubData;
import fl.engine.data.CountryData;
import fl.engine.data.DataAccessor;
import fl.engine.data.PlayerData;
import fl.engine.utils.NameGenerator;
import fl.engine.utils.PositionAnalyzer;
import fl.engine.utils.ProbabilisticNameGenerator;
import fl.engine.utils.WageModel;
import fl.engine.utils.YouthUtil;
import fl.utils.MathUtil;
import fl.utils.ScoredObject;

public class YouthGenerator extends BaseComponent {
	protected YouthGenerator(DataAccessor data_accessor, ComponentHub component_hub) {
		super(data_accessor, component_hub);
	}

	public void PromoteYouth() {
		PrepareData();
		GetComponentHub().RefreshWageModel(GetAccessor());
		
		int num_candidate_base = Constant.GetConstant().youth_promotion_per_year;
		long processed = 0L;
		for (ClubData club : GetAccessor().GetAllClubs()) {
			double wage_rank = club_wage_rank.get(club).doubleValue();
			int num_candidate = 
					YouthUtil.GetNumberCandidates(num_candidate_base, club.GetInfo().youth_training_level, 1);
			List<ScoredObject<PlayerInfo>> scored_candidates = new ArrayList<ScoredObject<PlayerInfo>>();
			for (int i = 0; i < num_candidate; ++i) {
				PlayerInfo player_info = YouthUtil.InitilaizeOnePlayer(club.GetInfo().youth_training_level);			
				scored_candidates.add(new ScoredObject<PlayerInfo>(player_info,
						PositionAnalyzer.GetPositionConfidence(player_info,
								player_info.GetPosition().type)));
			}
			scored_candidates.sort(ScoredObject.SortByScore());
			for (int i = 0; i < num_candidate_base; ++i) {
				PlayerInfo player_info = scored_candidates.get(i).element;
				int wage = (int) Math.max(WageModel.kMinWage,
						MathUtil.RoudByThousand((long)
						GetComponentHub().wage_model.Predicte(player_info, wage_rank)));
				PlayerData player = PromoteOneYouthPlayer(player_info, club, wage);
				player.WriteInfo();
				player.WriteData();
			}
			DataAccessor.SetProgress((float)++processed / GetAccessor().GetAllClubSize());
		}
	}
	
	public PlayerData PromoteOneYouthPlayer(PlayerInfo player_info, ClubData club, int wage) {
		CountryData country = club.GetCountry();
		PlayerData player = GetAccessor().AddPlayer();
		player_info.club_id = club.GetKey();
		player_info.club_info = player_info.new PlayerClubInfo();
		player_info.club_info.contract = 3;
		player_info.club_info.wage = wage;
		player.SetInfo(player_info);
		player.GetData().name = name_generator_.GenerateOneName(country.GetData());
		player.GetData().birth_year = GetAccessor().GetGlobal().year - 16;
		return player;
	}
	
	private void PrepareData() {
		List<ScoredObject<ClubData>> club_average_wage = new ArrayList<ScoredObject<ClubData>>();
		for (ClubData club : GetAccessor().GetAllClubs()) {
			double total_wage = 0;
			int total_player = 0;
			for (PlayerData player : club.GetPlayers()) {
				if (player.GetInfo().club_info.wage != null) {
					total_wage += player.GetInfo().club_info.wage;
					total_player++;
				}
			}
			club_average_wage.add(new ScoredObject<ClubData>(club, total_wage / total_player));
		}
		club_average_wage.sort(ScoredObject.SortByScoreAscending());
		for (int i = 0; i < club_average_wage.size(); ++i) {
			club_wage_rank.put(club_average_wage.get(i).element,
					Math.min(0.975, Math.max(0.025, (double)i / club_average_wage.size())));
		}
	}
	
	private NameGenerator name_generator_ =
			new ProbabilisticNameGenerator("constants/first_initial.csv", "constants/last_name.csv");
	private Map<ClubData, Double> club_wage_rank = new HashMap<ClubData, Double>();
}
