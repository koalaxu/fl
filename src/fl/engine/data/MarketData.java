package fl.engine.data;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fl.data.Position;

public class MarketData {
	public class Demand {
		private Demand() {}
		public Demand(ClubData club, Position position, int level, double priority) {
			this.club = club;
			this.position = position;
			this.level = level;
			this.priority = priority;
		}
		public ClubData club;
		public Position position;
		public double min_score;
		public int max_wage;
		public long max_transfer_fee;
		public int level;
		public double priority;
	}
	
	public class Supply {
		public Supply(PlayerData player, double score, int level) {
			this.player = player;
			this.score = score;
			this.level = level;
		}
		public PlayerData player;
		public int level;
		public double score;
		public int min_wage;
		public long min_transfer_fee;
	}
	
	public class ClubPlan {
		public ClubPlan(double average_score, long transfer_budget, long wage_budget, boolean rich) {
			this.average_score = average_score;
			this.transfer_budget = transfer_budget;
			this.wage_budget = wage_budget;
			this.rich = rich;
		}
		public double average_score;
		public long transfer_budget;
		public long wage_budget;
		public boolean rich;
	}
	
	private static class SupplyComparator implements Comparator<Supply> {
		@Override
		public int compare(Supply o1, Supply o2) {
			if (o1.score > o2.score) return -1;
			if (o1.score < o2.score) return 1;
			return 0;
		}
	}
	
	private static class DemandPriorityComparator implements Comparator<Demand> {
		@Override
		public int compare(Demand o1, Demand o2) {
			if (o1.level < o2.level) return -1;
			if (o1.level > o2.level) return 1;
			if (o1.priority > o2.priority) return -1;
			if (o1.priority < o2.priority) return 1;
			return 0;
		}
	}
	
	private static class DemandComparator implements Comparator<Demand> {
		public DemandComparator(boolean rank_by_price) {
			this.rank_by_price = rank_by_price;
		}
		@Override
		public int compare(Demand o1, Demand o2) {
			return rank_by_price ? compareByPrice(o1, o2) : compareByWage(o1, o2);
		}
		
		private int compareByPrice(Demand o1, Demand o2) {
			if (o1.max_transfer_fee > o2.max_transfer_fee) return -1;
			if (o1.max_transfer_fee < o2.max_transfer_fee) return 1;
			return 0;
		}
		
		private int compareByWage(Demand o1, Demand o2) {
			if (o1.max_wage > o2.max_wage) return -1;
			if (o1.max_wage < o2.max_wage) return 1;
			return 0;
		}
		
		boolean rank_by_price;  // or wage
	}
	
	public Demand GetMaxWageTheshold(int max_wage) {
		Demand demand = new Demand();
		demand.max_wage = max_wage;
		return demand;
	}
	
	public Demand GetMaxTransferFeeTheshold(long fee) {
		Demand demand = new Demand();
		demand.max_transfer_fee = fee;
		return demand;
	}
	
	public Supply FindSupply(PlayerData player) {
		if (player == null) return null;
		for (Supply supply : supplies) {
			if (supply.player.equals(player)) {
				return supply;
			}
		}
		return null;
	}
	
	public Map<ClubData, List<Demand>> club_demands = new HashMap<ClubData, List<Demand>>();
	public Set<Supply> supplies = new HashSet<Supply>();
	public Map<ClubData, ClubPlan> club_plans = new HashMap<ClubData, ClubPlan>();
	
	public static Comparator<Supply> SupplyByScoreDesc = new SupplyComparator();
	public static Comparator<Demand> DemandByPriceDesc = new DemandComparator(true);
	public static Comparator<Demand> DemandByWageDesc = new DemandComparator(false);
	public static Comparator<Demand> DemandByPriority = new DemandPriorityComparator();
}
