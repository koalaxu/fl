package fl.engine.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import fl.data.Global;
import fl.data.Position;
import fl.engine.data.DataAccessor;
import fl.engine.data.PlayerData;
import fl.engine.data.TransferData;
import fl.utils.OneParameterModel;
import fl.utils.QuantileCalculator;

public class PriceModel {
	public PriceModel(Global global) {
		for (int i = 0; i < Position.PositionName.values().length; ++i) {
			Position pos = Position.positions.get(Position.PositionName.values()[i]);
			// Overall score, contract length
			OneParameterModel model = new OneParameterModel();
			model.alpha = default_weights[i][0];
			model.beta = default_weights[i][1];
			models.put(pos, model);
		}
		this.global = global;
	}
	
	public void UpdatePriceBase(long old_fund, long new_fund) {
		System.err.println("Old fund: " + old_fund + "; New Fund: " + new_fund);
		global.base_price *= Math.min(1.1, Math.max(1.0 / 1.1, (double)new_fund / old_fund));
	}
	
	public void UpdatePriceBias(DataAccessor accessor) {
		Map<Position, QuantileCalculator> pos_counters = new HashMap<Position, QuantileCalculator>();
		for (Position pos : Position.positions.values()) {
			pos_counters.put(pos, new QuantileCalculator(null));
		}
		for (TransferData transfer : accessor.GetAllTransfers(global.year)) {
			long transfer_fee = transfer.GetFee();
			if (transfer_fee == 0L) continue;
			int contract_length = transfer.GetContractLength();
			PlayerData player = transfer.GetPlayer();
			double score = PositionAnalyzer.GetOverallScore(player.GetInfo());
			int age = player.GetAge();
			Position pos = player.GetPosition();
			double bias = InferPriceBias(transfer_fee, GetFeature0(player, score), GetFeature1(contract_length),
					GetFeature2(age));
			pos_counters.get(pos).AddOneElement(bias);
			pos = player.GetInfo().GetSecondaryPosition();
			if (pos != null) {
				pos_counters.get(pos).AddOneElement(bias);
			}
		}
		double average_bias = 0.0;
		for (Entry<Position, QuantileCalculator> entry : pos_counters.entrySet()) {
			Position pos = entry.getKey();
			int pos_index = pos.name.ordinal();
			QuantileCalculator cal = entry.getValue();
			if (cal.GetSize() >= kMinExamplesToUpdate) {
				average_bias += cal.GetMean() * position_weights[pos_index];
			} else {
				average_bias += global.position_bias[pos_index] * position_weights[pos_index];
			}
		}
		for (Entry<Position, QuantileCalculator> entry : pos_counters.entrySet()) {
			Position pos = entry.getKey();
			int pos_index = pos.name.ordinal();
			QuantileCalculator cal = entry.getValue();
			double old_bias = global.position_bias[pos_index];
			double new_bias =  (cal.GetSize() >= kMinExamplesToUpdate ?
					cal.GetMean() : global.position_bias[pos_index]) / average_bias;
			global.position_bias[pos_index] = Math.min(2.0, Math.max(0.5,
					old_bias * Math.min(1.05, Math.max(1.0 / 1.05, new_bias / old_bias))));
			if (Double.isNaN(global.position_bias[pos_index])) {
				global.position_bias[pos_index] = old_bias;
			}
			System.err.println(pos.name + " : " + old_bias + " -> " + global.position_bias[pos_index]);
		}
	}
	
	public void AddOneTransfer(PlayerData player, double score, int age, int contract_length, long transfer_fee) {
		Position pos = player.GetInfo().GetPosition();
		transfer_fee /= GetFeature1(contract_length) * GetFeature2(age);
		models.get(pos).AddData(transfer_fee, GetFeature0(player, score));
		pos = player.GetInfo().GetSecondaryPosition();
		if (pos != null) {
			models.get(pos).AddData(transfer_fee, GetFeature0(player, score));
		}
	}
	
	public long Predict(PlayerData player) {
		long fee = PredictForPosition(player, player.GetInfo().GetPosition());
		if (player.GetInfo().GetSecondaryPosition() != null) {
			fee = Math.max(fee, PredictForPosition(player, player.GetInfo().GetSecondaryPosition()));
		}
		return fee;
	}
	
	public long Predict(Position pos, double overall_score) {
		return PredictWithCalibration(pos, GetFeature0(overall_score), GetFeature1(3), GetFeature2(25));
	}
	
	public void Learn() {
		for (Entry<Position, OneParameterModel> entry : models.entrySet()) {
			OneParameterModel model = entry.getValue();
			if (!model.Process()) {
				System.err.println("The price model for " + entry.getKey().name + " cannot be trained. " +
						"Using default parameters.");
				continue;
			}
			System.err.println("PriceModel(" + entry.getKey().abbreviate + ") = " + model.alpha + ", " + model.beta);
		}
	}
	
	private long PredictForPosition(PlayerData player, Position pos) {
		return PredictWithCalibration(pos, GetFeature0(player), GetFeature1(player), GetFeature2(player));
	}
	
	private long PredictWithCalibration(Position pos, double feature0, double feature1, double feature2) {
		int pos_index = pos.name.ordinal();
		return (long) (global.base_price * global.position_bias[pos_index] * feature0 * feature1 * feature2);
	}
	
	private double InferPriceBias(long transfer_fee, double feature0, double feature1, double feature2) {
		return (double)transfer_fee / (global.base_price * feature0 * feature1 * feature2);
	}
	
	private double GetFeature0(double score) {
		return Math.pow(Math.max(0.1, (score - 0.45) / 0.2), 3.6);
	}
	
	private double GetFeature0(PlayerData player, double base_score) {
		double score = ability_estimator.EstimateFutureScore(player, base_score, 2);
		return GetFeature0(score);
	}
	
	private double GetFeature0(PlayerData player) {
		double score = ability_estimator.EstimateFutureScore(player, 2);
		return GetFeature0(score);
	}
	
	private double GetFeature1(int contract_length) {
		return Math.pow((double)contract_length / 3, 0.5);
	}
	
	private double GetFeature1(PlayerData player) {
		return GetFeature1(player.GetInfo().club_info.contract);
	}
	
	private double GetFeature2(int age) {
		return age < 29 ? 1.0 : ( age >= 33 ? 0.2 : Math.pow(0.6, age - 28));
	}
	
	private double GetFeature2(PlayerData player) {
		return GetFeature2(player.GetAge());
	}
	
	public static long kMinTransferFee = 1000;
	private static int kMinExamplesToUpdate = 20;
		
	private Map<Position, OneParameterModel> models = new HashMap<Position, OneParameterModel>();
	private AbilityEstimator ability_estimator = new AbilityEstimator();
	private Global global;
	private double[][] default_weights = {
			{ 8.3 + 9.3, 2 },
			{ 8.5 + 9.3, 2 },
			{ 8.7 + 9.3, 2 },
			{ 8.6 + 9.3, 2 },
			{ 8.4 + 9.3, 2 },
			{ 8.7 + 9.3, 2 },
			{ 9.0 + 9.3, 2 },
			{ 8.8 + 9.3, 2 },
			{ 8.6 + 9.3, 2 },
			{ 9.1 + 9.3, 2 },
			{ 9.2 + 9.3, 2 },
			{ 9.2 + 9.3, 2 },
			{ 9.2 + 9.3, 2 },
			{ 9.2 + 9.3, 2 },
	};
	private static double[] position_weights = {
			3.0 / 23,
			0.18 / 23,
			4.12 / 23,
			1.88 / 23,
			1.88 / 23,
			0.6 / 23,
			3.76 / 23,
			1.24 / 23,
			1.24 / 23,
			0.6 / 23,
			2.0 / 23,
			1.26 / 23,
			0.62 / 23,
			0.62 / 23,
	};
}
