package fl.engine.utils;

import fl.engine.data.PlayerData;
import fl.utils.RandomUtil;

public class InjuryUtil {
	public static boolean Injured(double base_probability, PlayerData player) {
		return RandomUtil.WhetherToHappend(base_probability * PlayerInjuryProbability(player));
	}
	
	public static int InjuryTime() {
		return (int) Math.ceil(Math.pow(RandomUtil.SampleFromNormalDistribution(0.0, 2.0), 2) * 2);
	}
	
	private static double PlayerInjuryProbability(PlayerData player) {
		return kInjuryProbabilities[(int) ((player.GetKey() * kRandomSalt) % 4)];
	}
	
	private static double[] kInjuryProbabilities = { 1.0, 0.5, 0.25, 0.125 };
	private static int kRandomSalt = 101;
}
