package fl.engine.utils;

import java.util.HashMap
;
import java.util.Map;
import java.util.Map.Entry;

import fl.data.PlayerInfo;
import fl.data.Position;
import fl.utils.OneParameterModel;

public class WageModel {
	public WageModel() {
		for (int i = 0; i < Position.PositionName.values().length; ++i) {
			Position pos = Position.positions.get(Position.PositionName.values()[i]);
			OneParameterModel model = new OneParameterModel();
			model.alpha = kDefaultWeights[i][0];
			model.alpha_stderr = kDefaultAlphaError;
			model.beta = kDefaultWeights[i][1];
			model.beta_stderr = kDefaultBetaError;
			models.put(pos, model);
		}
	}
	public void AddOnePlayerToModel(PlayerInfo player) {
		if (player.club_info == null || player.club_info.wage == null) return;
		double score = PositionAnalyzer.GetOverallScore(player, player.GetPosition());
		if (player.secondary_position != null) {
			score = Math.max(score, PositionAnalyzer.GetOverallScore(player, player.GetSecondaryPosition())); 
		}
		models.get(player.GetPosition()).AddData(player.club_info.wage, GetFeature(score));
	}
	
	public void Learn() {
		for (Entry<Position, OneParameterModel> entry : models.entrySet()) {
			OneParameterModel model = entry.getValue();
			model.Process();
			System.err.println("WageModel(" + entry.getKey().abbreviate + ") = " + model.alpha + ", " + model.beta);
		}
	}
	
	public double Predicte(PlayerInfo player, double bias) {
		double score = PositionAnalyzer.GetOverallScore(player, player.GetPosition());
		if (player.secondary_position != null) {
			score = Math.max(score, PositionAnalyzer.GetOverallScore(player, player.GetSecondaryPosition())); 
		}
		OneParameterModel model = models.get(player.GetPosition());
		return model.Predict(bias, GetFeature(score));
	}
	
	public double Predicte(Position pos, double overall_score) {
		return models.get(pos).Predict(0.5, GetFeature(overall_score));
	}
	
	private double GetFeature(double score) {
		return Math.pow(Math.max(0.1, (score - 0.4) / 0.25), 3.6);
	}
	
	private Map<Position, OneParameterModel> models = new HashMap<Position, OneParameterModel>();
	
	public static int kMinWage = 5000;
	
	private double[][] kDefaultWeights = {
			{ 8.3 + 3.7, 1},
			{ 8.5 + 3.7, 1},
			{ 8.7 + 3.7, 1},
			{ 8.6 + 3.7, 1},
			{ 8.4 + 3.7, 1},
			{ 8.7 + 3.7, 1},
			{ 8.9 + 3.7, 1},
			{ 8.8 + 3.7, 1},
			{ 8.6 + 3.7, 1},
			{ 9.0 + 3.7, 1},
			{ 9.1 + 3.7, 1},
			{ 9.1 + 3.7, 1},
			{ 9.1 + 3.7, 1},
			{ 9.1 + 3.7, 1},
	};
	
	private static double kDefaultAlphaError = 0.5;
	private static double kDefaultBetaError = 0;
}
