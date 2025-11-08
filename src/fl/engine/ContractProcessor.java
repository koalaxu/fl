package fl.engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import fl.data.Constant;
import fl.data.PlayerInfo.PlayerClubInfo;
import fl.data.PlayerLeagueRecord;
import fl.engine.ControllerModule.RenewalStatus;
import fl.engine.data.ClubData;
import fl.engine.data.DataAccessor;
import fl.engine.data.LeagueData;
import fl.engine.data.PlayerData;
import fl.engine.utils.PositionAnalyzer;
import fl.engine.utils.WageModel;
import fl.engine.utils.YouthUtil;
import fl.utils.MathUtil;
import fl.utils.ScoredObject;

public class ContractProcessor extends BaseComponent {

	protected ContractProcessor(DataAccessor data_accessor, ComponentHub component_hub) {
		super(data_accessor, component_hub);
	}
	
	private static class ExpiringPlayer {
		public ExpiringPlayer(PlayerData player, double score) {
			this.player = player;
			current_wage = player.GetInfo().club_info.wage;
		}
		public PlayerData player;
		public int max_wage;
		public int min_wage;
		public int current_wage;
		public int[] wage_thresholds;
		public int offer;
		public double weight;
	}
	
	public int RenewContract() {
		int retired = 0;
		GetComponentHub().RefreshWageModel(GetAccessor());
		long processed = 0;
		for (ClubData club : GetAccessor().GetAllClubs()) {
			for (PlayerData player : club.GetPlayers()) {
				if (player.GetInfo().club_info.contract <= 1 && YouthUtil.WhetherToRetire(player)) {
					player.DeleteInfo();
					retired++;
				}
			}
			DataAccessor.SetProgress((float)++processed / GetAccessor().GetAllClubSize() / 2.0f);
		}
		for (ClubData club : GetAccessor().GetAllClubs()) {
			ProceedOneClub(club);
			DataAccessor.SetProgress((float)++processed / GetAccessor().GetAllClubSize() / 2.0f);
		}
		return retired;
	}
	
	public RenewalStatus RenewContract(PlayerData player, int wage, int contract_length) {
		ScoredObject<PlayerData> scored_player =
				new ScoredObject<PlayerData>(player, PositionAnalyzer.GetOverallScore(player.GetInfo()));
		ExpiringPlayer demand = CreateCandidate(player.GetClub(), scored_player);
		if (contract_length <= 0 || contract_length > demand.wage_thresholds.length ||
				contract_length + player.GetInfo().club_info.contract > 6) {
			return RenewalStatus.REJECTED_LENGTH;
		}
		if (wage < demand.wage_thresholds[contract_length - 1]) {
			return RenewalStatus.REJECTED_WAGE;
		}
		SignNewContract(player, wage, contract_length);
		return RenewalStatus.ACCEPTED;
	}
	
	private void ProceedOneClub(ClubData club) {
		if (club.equals(GetAccessor().GetControlledClub())) return;
		long wage_budget = (long) (club.GetInfo().estimated_income * Constant.GetConstant().max_wage_income_ratio
				+ Math.max(0, club.GetInfo().fund * Constant.GetConstant().max_wage_balance_ratio));
		List<ScoredObject<PlayerData>> players = new ArrayList<ScoredObject<PlayerData>>();
		for (PlayerData player : club.GetPlayers()) {
			players.add(new ScoredObject<PlayerData>(player, PositionAnalyzer.GetOverallScore(player.GetInfo())));
		}
		players.sort(ScoredObject.SortByScore());
		List<ExpiringPlayer> candidates = new ArrayList<ExpiringPlayer>();
		
		for (int i = 0; i < players.size(); ++i) {
			PlayerData player = players.get(i).element;
			if (player.GetInfo().club_info.contract <= 2 && i < 22) {
				candidates.add(CreateCandidate(club, players.get(i)));
			}
			if (i < 25 || player.GetInfo().club_info.contract > 1) {
				wage_budget -= player.GetInfo().club_info.wage;
			}
		}
		if (candidates.isEmpty()) return;
		if (wage_budget <= 0) return;
		long remaining_budget = RenewCandidates(wage_budget,
				Math.max(0L, (long)(club.GetInfo().fund * Constant.GetConstant().backup_wage_budget_multiplier)),
				candidates);
		if (remaining_budget > 0) {
			int remaining_players = 0;
			for (int i = 0; i < players.size() && i < 22; ++i) {
				PlayerData player = players.get(i).element;
				if (player.GetInfo().club_info.contract > 1) remaining_players++;
			}
			for (int i = 22; remaining_players < 28 && i < players.size(); ++i) {
				if (players.get(i).element.GetAge() > 22 ||
						players.get(i).element.GetInfo().club_info.contract > 2) continue;
				ExpiringPlayer player = CreateCandidate(club, players.get(i));
				if (remaining_budget < player.min_wage) continue;
				int delta = SignNewContract(player, (int) Math.min(remaining_budget, player.max_wage));
				remaining_budget -= delta;
				remaining_players++;
			}
		}
	}
	
	private ExpiringPlayer CreateCandidate(ClubData club, ScoredObject<PlayerData> scored_player) {
		PlayerData player = scored_player.element;
		int games = 0;
		double score = 0.0;
		for (LeagueData league : club.GetClubParticipatingLeagues()) {
			PlayerLeagueRecord record = player.GetLeagueInfo(league).GetLeagueRecord(GetAccessor().GetGlobal().year);
			if (record == null) continue;
			score += record.average_score;
			games += record.match_played;
		}
		ExpiringPlayer candidate = new ExpiringPlayer(player, scored_player.score);
		int current_wage = player.GetInfo().club_info.wage;
		double base = kMinRatio;
		if (games > 0) {
			score /= games;
			if (score > 55) {
				base = Math.pow(2, (score - 55) * 0.15) * 0.1 + 1;
			} else if (score > 45) {
				base = 1.2 - Math.pow(2, (55 - score) * 0.2) * 0.1;
			}
		}
		
		int max_contract_length = Math.min(
				Math.max(4, 6 - player.GetInfo().club_info.contract),
				Math.max(1, 32 - player.GetAge()));
		candidate.min_wage = (int) (current_wage * (base - 0.2));
		candidate.max_wage = (int) (current_wage * (base - 0.2 + max_contract_length * 0.1));
		candidate.wage_thresholds = new int[max_contract_length];
		for (int i = 0; i < max_contract_length; ++i) {
			candidate.wage_thresholds[i] = (int) (current_wage * Math.pow(base - 0.2, 0.5 * i + 1 ));
		}
		return candidate;
	}
	
	private long RenewCandidates(long budget, long backup_budget, List<ExpiringPlayer> candidates) {
		long cost_needed = 0L;
		long budget_pool = 0L;
		List<Double> weights = new ArrayList<Double>();
		for (ExpiringPlayer player : candidates) {
			cost_needed += player.min_wage - player.current_wage;
			budget_pool += player.current_wage;
			weights.add(GetComponentHub().wage_model.Predicte(player.player.GetInfo(), 0.5));
		}
		budget_pool += budget;
		if (cost_needed < budget) {
			// Budget is sufficient for minimum requirement.
			MathUtil.Normalize(weights);
			for (int i = 0; i < candidates.size(); ++i) {
				ExpiringPlayer player = candidates.get(i);
				player.offer = (int)(budget_pool * weights.get(i));
				player.weight = weights.get(i);
			}
			candidates.sort(new DeltaComparator());
			double remaining_weight = 1.0;
			for (int i = 0; i < candidates.size(); ++i) {
				ExpiringPlayer player = candidates.get(i);
				int delta = 0;
				if (player.offer < player.min_wage) {
					delta = SignNewContract(player, player.min_wage);
				} else {
					delta = SignNewContract(player, Math.min(player.offer, player.max_wage));
				}
				budget_pool -= player.player.GetInfo().club_info.wage;
				budget -= delta;
				remaining_weight -= player.weight;
				for (int j = i + 1; j < candidates.size(); ++j) {
					ExpiringPlayer ep =	candidates.get(j);
					ep.offer = (int) (budget_pool * ep.weight / remaining_weight); 
				}
			}
			return budget;
		}
		budget += backup_budget;
		for (ExpiringPlayer player : candidates) {
			if (budget >= player.min_wage) {
				int delta = SignNewContract(player, player.min_wage);
				budget -= delta;
			}
		}
		return budget;
	}
		
	private int SignNewContract(ExpiringPlayer player, int wage) {
		int contract_length = GetContractLength(player, wage);
		int actual_wage = (int) Math.max(WageModel.kMinWage, MathUtil.RoundBy(wage, 100));
		return SignNewContract(player.player, actual_wage, contract_length);
	}
	
	private int SignNewContract(PlayerData player, int wage, int contract_length) {
		PlayerClubInfo info = player.GetInfo().club_info;
		info.contract += contract_length;
		info.wage = wage;
		int delta = wage - info.wage;
		player.WriteInfo();
		return delta;
	}
	
	private int GetContractLength(ExpiringPlayer player, int wage) {
		for (int i = player.wage_thresholds.length - 1; i >= 0; --i) {
			if (wage >= player.wage_thresholds[i]) return i + 1;
		}
		return 1;
	}
	
	private static class DeltaComparator implements Comparator<ExpiringPlayer> {

		@Override
		public int compare(ExpiringPlayer o1, ExpiringPlayer o2) {
			int delta1 = o1.offer - o1.min_wage;
			int delta2 = o2.offer - o1.min_wage;
			if (delta1 < delta2) return -1;
			if (delta1 > delta2) return 1;
			return 0;
		}
		
	}
	
	private static double kMinRatio = 0.8;
}
