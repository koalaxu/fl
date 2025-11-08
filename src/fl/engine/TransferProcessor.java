package fl.engine;

import java.util.ArrayList;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.SortedMap;

import fl.data.ClubInfo;
import fl.data.ClubRecord;
import fl.data.Constant;
import fl.data.Formation;
import fl.data.PlayerInfo;
import fl.data.PlayerInfo.PlayerClubInfo;
import fl.data.Position;
import fl.engine.ControllerModule.BidStatus;
import fl.engine.data.ClubData;
import fl.engine.data.DataAccessor;
import fl.engine.data.MarketData;
import fl.engine.data.PlayerData;
import fl.engine.data.TransferData;
import fl.engine.data.MarketData.ClubPlan;
import fl.engine.data.MarketData.Demand;
import fl.engine.data.MarketData.Supply;
import fl.engine.utils.AbilityEstimator;
import fl.engine.utils.FormationUtil;
import fl.engine.utils.PositionAnalyzer;
import fl.engine.utils.PriceModel;
import fl.engine.utils.WageModel;
import fl.utils.MathUtil;
import fl.utils.ScoredObject;
import fl.utils.TopN;

public class TransferProcessor extends BaseComponent {

	protected TransferProcessor(DataAccessor data_accessor, ComponentHub component_hub) {
		super(data_accessor, component_hub);
	}
	
	public void Preprocess(int round) {
		if (market == null) {
			market = GetAccessor().CreateNewMarketData();
			this.round = round;
			PreparePlayers();
			PrepareClubs();	
			UpdateSupplies();
		}
	}

	public int Process(int round) {
		market = GetAccessor().CreateNewMarketData();
		this.round = round;
		PreparePlayers();
		PrepareClubs();	
		UpdateSupplies();
		UpdateDemands();
		int total = 0;
		int processed = 0;
		for (Position position : Position.positions.values()) {
			total += ProceedOnePosition(position);
			DataAccessor.SetProgress((++processed / Position.positions.size()) / 1.2f);
		}
		if (round == 0) {
			GetComponentHub().price_model.UpdatePriceBias(GetAccessor());
		}
		market = null;
		return total;
	}
	
	public void OverrideMinTransferFee(PlayerData player, long fee) {
		Supply supply = market.FindSupply(player);
		if (supply == null) return;
		supply.min_transfer_fee = fee;
	}
	
	private void OverrideMinTransferFees() {
		Map<Long, Long> min_transfer_fee_overrides = GetAccessor().GetGlobal().min_transfer_fee_override;
		for (Entry<Long, Long> min_transfer_fee_override : min_transfer_fee_overrides.entrySet()) {
			long playey_id = min_transfer_fee_override.getKey();
			OverrideMinTransferFee(GetAccessor().GetPlayer(playey_id), min_transfer_fee_override.getValue());
		}
	}
	
	private void PreparePlayers() {
		transferred_players = new HashSet<PlayerData>();
		for (TransferData transfer : GetAccessor().GetAllTransfers(GetAccessor().GetGlobal().year)) {
			transferred_players.add(transfer.GetPlayer());
		}
	}
	
	private void PrepareClubs() {
		long processed = 0L;
		for (ClubData club : GetAccessor().GetAllClubs()) {
			market.club_demands.put(club, new ArrayList<Demand>());
			ClubPlan plan = EvaluatePlayers(club);
			if (plan != null) market.club_plans.put(club, plan);
			DataAccessor.SetProgress((float)++processed / GetAccessor().GetAllClubSize());
		}
	}
	
	private long AllocateWageBudget(ClubData club) {
		SortedMap<Integer, ClubRecord> records = club.GetRecords();
		long total_income = 0L;
		final int traceback_years = 5;
		for (int i = 1; i <= traceback_years; ++i) {
			ClubRecord record = records.get(GetAccessor().GetGlobal().year - i);
			if (record != null) total_income += record.income;
			else total_income += club.GetInfo().estimated_income;
		}
		return  (long) (Math.min(total_income / traceback_years, club.GetInfo().estimated_income) *
				Constant.GetConstant().max_wage_income_ratio);
	}
	
	private ClubPlan EvaluatePlayers(ClubData club) {
		ClubInfo club_info = club.GetInfo();
		long wage_fund = AllocateWageBudget(club);
		long transfer_fund = club_info.fund;
		long wage_cost = GetComponentHub().cost_calculator.GetClubCost(club);
		boolean rich = (wage_fund + transfer_fund) > wage_cost * 1.2;
		long wage_budget = wage_fund - wage_cost;
		boolean debt = club_info.estimated_income < wage_cost;
		if (transfer_fund + wage_budget > 0 && (wage_budget - wage_cost) < transfer_fund * kMinWageFeeRatio) {
			long shift = (long) ((transfer_fund - wage_budget / kMinWageFeeRatio) / (1.0 / kMinWageFeeRatio + 1));
			if (shift > 0) {
				wage_budget += shift;
				transfer_fund -= shift;
				if (transfer_fund < 0) {
					System.err.println("Transfer fund is negative. Shouldn't happen.");
					System.exit(0);
				}
			}
		}
		Formation formation = club.GetInfo().GetFavoriteFormation();
		List<Position> positions = FormationUtil.GetFormationPositions(formation);
		Map<Position, Integer> pos_needs = new HashMap<Position, Integer>();
		Map<Position, List<ScoredObject<PlayerData>>> pos_candidates =
				new HashMap<Position, List<ScoredObject<PlayerData>>>();
		Set<PlayerData> remaining_players = new HashSet<PlayerData>();
		remaining_players.addAll(club.GetPlayers());
		// Add out of contract players to market
		for (PlayerData player : club.GetPlayers()) {
			if (player.GetInfo().club_info != null && player.GetInfo().club_info.contract > 0) continue;
			remaining_players.remove(player);
			AddPlayerToSupply(player, Integer.MAX_VALUE, debt);
		}
		// Evaluate Positions
		for (Position pos : positions) {
			pos_needs.put(pos, pos_needs.getOrDefault(pos, 0) + 1);
			pos_candidates.put(pos, new ArrayList<ScoredObject<PlayerData>>());
		}
		List<Position> ordered_positions = new ArrayList<Position>();
		for (int i = 0; i < 2; ++i) {
			for (Entry<Position, Integer> entry : pos_needs.entrySet()) {
				Position pos = entry.getKey();
				for (int j = 0; j < entry.getValue(); ++j) {
					ordered_positions.add(pos);
				}
			}
		}
		ordered_positions.add(Position.GoalKeeper);
		Set<PlayerData> players_with_position = new HashSet<PlayerData>();
		int rank = 0;
		double total_score = 0;
		int total_num = 0;
		for (int i = 0; i < ordered_positions.size(); ++i) {
			Position pos = ordered_positions.get(i);
			PlayerData selected_player = null;
			double max_score = -1;
			for (PlayerData player : remaining_players) {
				if (player.GetInfo().GetPosition() != pos &&
						player.GetInfo().GetSecondaryPosition() != pos) continue;
				players_with_position.add(player);
				double score = PositionAnalyzer.GetPositionConfidence(player.GetInfo(), pos.type);
				if (score > max_score) {
					selected_player = player;
					max_score = score;
				}
			}
			if (selected_player != null) {
				remaining_players.remove(selected_player);
				ScoredObject<PlayerData> scored_player =
						new ScoredObject<PlayerData>(selected_player, max_score);
				pos_candidates.get(pos).add(scored_player);
				AddPlayerToSupply(selected_player, rank++, debt);
				if (i < 11) {
					total_score += max_score; 
					total_num++;
				}
			}
		}
		double average_score = total_score / total_num;
		List<ScoredObject<PlayerData>> player_scores = new ArrayList<ScoredObject<PlayerData>>();
		for (PlayerData player : remaining_players) {
			player_scores.add(new ScoredObject<PlayerData>(
					player, (players_with_position.contains(player) ? 1.0 : 0.0) +
							ability_estimator.EstimateFutureScore(player, 3)));
		}
		player_scores.sort(ScoredObject.SortByScore());
		for (ScoredObject<PlayerData> scored_player : player_scores) {
			PlayerData player = scored_player.element;
			AddPlayerToSupply(player, rank++, debt);
		}
		if (wage_budget <= 0) return null;
		double score_discount = rich ? 1.0 : 0.9;
		double[] score_bars = { average_score * 0.9 * score_discount, average_score * 0.8 * score_discount,
				average_score * 0.7 * score_discount, average_score * 0.7 * score_discount};
		for (Entry<Position, Integer> entry : pos_needs.entrySet()) {
			Position pos = entry.getKey();
			int desired = entry.getValue();
			List<ScoredObject<PlayerData>> candidates = pos_candidates.get(pos);
			candidates.sort(ScoredObject.SortByScore());
			int index = 0;
			int fulfilled = 0;
			while (fulfilled < desired * 5 / 2) {
				ScoredObject<PlayerData> scored_player = index < candidates.size() ? candidates.get(index) : null;
				int lineup_level = fulfilled < desired ? 0 : (fulfilled < desired * 2 ? 1 : 2);
				if (scored_player != null && scored_player.score >= score_bars[lineup_level]) {
					if (lineup_level == 0 && scored_player.score < average_score * 1.1) {
						AddPositionToDemand(club, pos, 6, average_score * 1.1 - scored_player.score);
					}
					fulfilled++;
					index++;
					continue;
				}
				while (fulfilled + 1 < desired) {
					AddPositionToDemand(club, pos, lineup_level * 2 + 1,
							(fulfilled + 1 == desired && scored_player != null) ?
									score_bars[lineup_level] - scored_player.score :Double.MAX_VALUE);
					fulfilled++;
				}
				if (scored_player != null && scored_player.score >= score_bars[lineup_level + 1]) {
					AddPositionToDemand(club, pos,
							round < kAbilityDiscountRoundCutoff ? lineup_level * 2 + 1 : lineup_level * 2 + 2,
							score_bars[lineup_level] - scored_player.score);
					fulfilled += 2;
					index++;
					continue;
				}
				AddPositionToDemand(club, pos, lineup_level * 2 + 1, Double.MAX_VALUE);
				fulfilled++;
			}	
		}
		if (market.club_demands.get(club).isEmpty()) return null;
		PrioritizeDemands(club);
		return market.new ClubPlan(average_score, transfer_fund, wage_budget, rich);
 	}
	
	private void AddPlayerToSupply(PlayerData player, int rank, boolean debt) {
		if (transferred_players.contains(player)) {
			return;
		}		
		int level = 4;
		if (rank < 11) level = 1;
		else if (rank < 25) level = 2;
		else if (rank < 30) level = 3;
		level = Math.min(4, level + (debt ? 1 : 0));
		if (player.GetInfo().club_info == null || player.GetInfo().club_info.contract <= 0) {
			level = 0;
		} else if (player.GetInfo().club_info.contract >= 5) {
			level = 1;
		}
		if (!debt && round < kSupplyRoundCutoff[level] && level > 0) return;
		market.supplies.add(market.new Supply(player, ability_estimator.EstimateFutureScore(player, 1), level));
	}
	
	private void AddPositionToDemand(ClubData club, Position pos, int level, double priority) {
		if (club.GetInfo().fund < PriceModel.kMinTransferFee && level != 1 && level != 3) return;
		market.club_demands.get(club).add(market.new Demand(club, pos, level, priority));
	}
	
	private void PrioritizeDemands(ClubData club) {
		List<Demand> all_demands = market.club_demands.get(club);
		List<Demand> low_demand = new ArrayList<Demand>();
		List<Demand> high_demand = new ArrayList<Demand>();
		for (Demand demand : all_demands) {
			if (demand.level >= 6) low_demand.add(demand);
			else if (demand.level <= 3) high_demand.add(demand);
		}
		if (!high_demand.isEmpty()) {
			market.club_demands.get(club).retainAll(high_demand);
		} else if (all_demands.size() > low_demand.size()) {
			market.club_demands.get(club).removeAll(low_demand);
		}
		market.club_demands.get(club).sort(MarketData.DemandByPriority);
	}
	
	private void UpdateSupplies() {
		double discount_multiplier = round < kDiscountRoundCutoff ?
				(double)(kDiscountRoundCutoff - round) / kDiscountRoundCutoff : 0;
		for (Supply supply : market.supplies) {
			PlayerClubInfo club_info = supply.player.GetInfo().club_info;
			ClubPlan plan = market.club_plans.get(supply.player.GetClub());
			double trend_multiplier = ability_estimator.EstimateFutureScore(supply.player, 2) /
					ability_estimator.EstimateFutureScore(supply.player, 0);
			supply.min_wage = Math.max(WageModel.kMinWage, (int) (
					club_info.wage * trend_multiplier * (kWageExpectation[supply.level] -
							kWageDiscount[supply.level] * discount_multiplier)));
			if (supply.level > 0) {
				supply.min_transfer_fee = (long) (GetComponentHub().price_model.Predict(supply.player)
						* (kSalePriceExpectation[supply.level] -
								kPriceDiscount[supply.level] * discount_multiplier +
						((plan != null && plan.rich) ? kPriceBoost[supply.level] : 0)));
			} else {
				supply.min_transfer_fee = 0;
			}
		}
		OverrideMinTransferFees();
	}
	
	private void UpdateDemands() {
		double ability_discount_multiplier = round < kAbilityDiscountRoundCutoff ?
				(double)(kAbilityDiscountRoundCutoff - round) / kAbilityDiscountRoundCutoff : 0;
		for (Entry<ClubData, ClubPlan> entry : market.club_plans.entrySet()) {
			List<Demand> demands = market.club_demands.get(entry.getKey());
			if (entry.getKey().equals(GetAccessor().GetControlledClub())) {
				demands.clear();
				continue;
			}
			ClubPlan plan = entry.getValue();
			if (demands.isEmpty()) continue;
			int max_wage = GetMaxWage(entry.getKey());
			double[] price_weights = new double [demands.size()];
			double[] wage_weights = new double [demands.size()];
			long buffered_budget = (long) (plan.transfer_budget * (1.0 + kBudgetBuffer *
					(double)Math.min(kBudgetBufferRoundCutoff, round) / kBudgetBufferRoundCutoff));
			for (int i = 0; i < demands.size(); ++i) {
				Demand demand = demands.get(i);
				demand.min_score = plan.average_score * (kAbilityMultiplier[demand.level] -
						kAbilityDiscount[demand.level] * ability_discount_multiplier);
				double base_score = plan.average_score * kAbilityMultiplier[demand.level];
				double base_price = GetComponentHub().price_model.Predict(demand.position, base_score);
				double expected_price = base_price * kPriceExpectation[demand.level];
				if (buffered_budget > 0) {
					price_weights[i] = expected_price;
					buffered_budget -= expected_price;
				} else {
					price_weights[i] = 0;
				}
				wage_weights[i] = GetComponentHub().wage_model.Predicte(demand.position, base_score)
						* kPriceExpectation[demand.level];
			}
			MathUtil.Normalize(price_weights);
			MathUtil.Normalize(wage_weights);
			for (int i = 0; i < demands.size(); ++i) {
				Demand demand = demands.get(i);
				demand.max_transfer_fee = (long) (plan.transfer_budget * price_weights[i]);
				demand.max_wage = Math.min(max_wage, (int) (plan.wage_budget * wage_weights[i]));
			}
		}
	}
	
	private int GetMaxWage(ClubData club) {
		int max_wage = 0;
		for (PlayerData player : club.GetPlayers()) {
			max_wage = Math.max(max_wage, player.GetInfo().club_info.wage);
		}
		return (int) (max_wage * kMaxWageMultiplier);
	}
	
	private int ProceedOnePosition(Position position) {
		int count = 0;
		List<Supply> supplies = new ArrayList<Supply>();
		List<Demand> demands_wage_sorted = new ArrayList<Demand>();
		List<Demand> demands_price_sorted = new ArrayList<Demand>();
		for (Supply supply : market.supplies) {
			if (supply.player.GetInfo().GetPosition() == position ||
					supply.player.GetInfo().GetSecondaryPosition() == position) {
				supplies.add(supply);
			}
		}
		for (List<Demand> demands : market.club_demands.values()) {
			for (Demand demand : demands) {
				if (demand.position != position) continue;
				demands_wage_sorted.add(demand);
				demands_price_sorted.add(demand);
			}
		}
		supplies.sort(MarketData.SupplyByScoreDesc);
		demands_wage_sorted.sort(MarketData.DemandByWageDesc);
		demands_price_sorted.sort(MarketData.DemandByPriceDesc);
		for (Supply supply : supplies) {
			int min_wage = supply.min_wage;
			long min_price = supply.min_transfer_fee;
			int index = Collections.binarySearch(demands_wage_sorted,
					market.GetMaxWageTheshold(min_wage - 1), MarketData.DemandByWageDesc);
			Set<Demand> valid_demands = new HashSet<Demand>();
			if (index != -1) {  // -1 means inserting before 0, which indicates nobody qualifies.
				valid_demands.addAll(demands_wage_sorted.subList(0, Math.abs(index + 1)));
			}
			
			index = Collections.binarySearch(demands_price_sorted,
					market.GetMaxTransferFeeTheshold(min_price - 1), MarketData.DemandByPriceDesc);
			if (index != -1) {  // -1 means inserting before 0, which indicates nobody qualifies.
				valid_demands.retainAll(demands_price_sorted.subList(0, Math.abs(index + 1)));
			} else if (supply.min_transfer_fee > 0L) {
				valid_demands.clear();
			}
			valid_demands.removeIf(demand -> { return demand.min_score > supply.score ||
					demand.club.equals(supply.player.GetClub()); });
			if (valid_demands.isEmpty()) continue;
			long transfer_fee = supply.min_transfer_fee;
			List<Demand> candidates = new ArrayList<Demand>();
			if (supply.min_transfer_fee == 0L) {
				candidates.addAll(valid_demands);
			} else {
				PriorityQueue<Demand> sorted_demands = new PriorityQueue<Demand>(
						valid_demands.size(), MarketData.DemandByPriceDesc);
				sorted_demands.addAll(valid_demands);
				
				candidates.add(sorted_demands.poll());
				while (!sorted_demands.isEmpty()) {
					Demand candidate = sorted_demands.poll();
					if (candidate.max_transfer_fee > supply.min_transfer_fee * kMaxFeeMultiplier) {
						transfer_fee = (long) (supply.min_transfer_fee * kMaxFeeMultiplier);
						candidates.add(candidate);
					} else {
						transfer_fee = Math.max(transfer_fee, candidate.max_transfer_fee);  // Second price
						break;
					}
				}
			}
			candidates = TopN.FindSortedTopN(candidates, 2, MarketData.DemandByWageDesc);
			Demand fulfilled_demand = candidates.get(0);
			ClubData winner = fulfilled_demand.club;
			int wage = candidates.size() > 1 ? candidates.get(1).max_wage :
				(candidates.get(0).max_wage - supply.min_wage) / 10 + supply.min_wage;
			transfer_fee = MathUtil.RoudByThousand(transfer_fee);
			wage = (int) MathUtil.RoudByThousand(wage);
			
			// Update Market
			market.supplies.remove(supply);
			market.club_demands.get(winner).remove(fulfilled_demand);
			ClubPlan club_plan = market.club_plans.get(winner);
			club_plan.transfer_budget -= transfer_fee;
			for (Demand demand : market.club_demands.get(winner)) {
				demand.max_transfer_fee = Math.min(demand.max_transfer_fee, club_plan.transfer_budget);
			}
			
			demands_wage_sorted.remove(fulfilled_demand);
			demands_price_sorted.remove(fulfilled_demand);
			Transfer(supply, winner, transfer_fee, wage);
			count++;
		}
		return count;
	}
	
	private void Transfer(Supply supply, ClubData to_club, long transfer_fee, int wage) {
		PlayerData player = supply.player;
		// Record Transfer
		GetAccessor().CreateOneTransfer(TransferData.MakeTransfer(
				player, to_club, GetAccessor().GetGlobal().year, transfer_fee));
		
		// Update Player/Club
		ClubData from_club = player.GetClub();
		from_club.GetInfo().fund += transfer_fee;
		from_club.WriteInfo();
		to_club.GetInfo().fund -= transfer_fee;
		to_club.WriteInfo();
		PlayerInfo player_info = player.GetInfo();
		player_info.club_id = to_club.GetKey();
		player_info.club_info = player_info.new PlayerClubInfo();
		player_info.club_info.squad_number = null;
		player_info.club_info.wage = wage;
		player_info.club_info.contract = Math.min(5, Math.max(transfer_fee > 0 ? 2 : 1, 32 - player.GetAge()));
		player.WriteInfo();
	}
	
	public BidStatus Bid(Supply supply, ClubData club, long transfer_fee, int wage) {
		if (club.GetInfo().fund < transfer_fee) {
			return BidStatus.NO_SUFFICIENT_FEE;
		}
		if (supply.min_transfer_fee > transfer_fee) {
			return BidStatus.REJECTED_TRANSFER_FEE;
		}
		if (supply.min_wage > wage) {
			return BidStatus.REJECTED_WAGE;
		}
		Transfer(supply, club, transfer_fee, wage);
		return BidStatus.ACCEPTED;
	}
	
	private MarketData market;
	
	private int round;
	private Set<PlayerData> transferred_players;
	
	private AbilityEstimator ability_estimator = new AbilityEstimator();
	
	// Supply parameters
	private static double kWageExpectation[] = { 1.5, 1.2, 1.1, 1.0, 0.9 };
	private static double kSalePriceExpectation[] = { 0, 2.0, 1.6, 1.0, 0.8 };
	private static int kSupplyRoundCutoff[] = {  0, 8, 4, 0, 0 };
	private static int kDiscountRoundCutoff = 8;
	private static double[] kPriceBoost = { 0, 3.0, 2.0, 0, 0 };
	private static double[] kPriceDiscount = { 0, 0, 0, 0.2, 0.3 };
	private static double[] kWageDiscount = { 1.2, 0, 0, 0.2, 0.3 };
	// Demand parameters
	private static double kPriceExpectation[] = { Double.MAX_VALUE, 1.8, 1.4, 1.0, 0.8, 0.7, 1.2 };
	private static double kAbilityMultiplier[] = { 0, 0.85, 0.875, 0.8, 0.825, 0.75, 1.1 }; 
	private static int kAbilityDiscountRoundCutoff = 6;
	private static double[] kAbilityDiscount = { 0, 0.15, 0, 0.15, 0.0, 0.0, 0.0 };
	private static double kBudgetBuffer = 0.2;
	private static int kBudgetBufferRoundCutoff = 8;
	
	private static double kMaxWageMultiplier = 1.1;
	private static double kMaxFeeMultiplier = 2.0;
	
	private static double kMinWageFeeRatio = 0.2;
}
