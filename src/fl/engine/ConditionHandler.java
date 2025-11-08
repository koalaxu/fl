package fl.engine;

import fl.data.Constant;
import fl.data.PlayerInfo;
import fl.engine.data.DataAccessor;
import fl.engine.data.PlayerData;
import fl.utils.FieldAccessor;
import fl.utils.RandomUtil;

public class ConditionHandler extends BaseComponent {
	protected ConditionHandler(DataAccessor data_accessor, ComponentHub component_hub) {
		super(data_accessor, component_hub);
	}

	public void Proceed() {
		for (PlayerData player : GetAccessor().GetAllActivePlayers()) {
			PlayerInfo info = player.GetInfo();
			boolean edited = false;
			if (FieldAccessor.kZeroBased.Get(info.injury) > 0) {
				info.injury = FieldAccessor.kZeroBased.Update(info.injury, -1);
				info.condition = kConditionAccessor.Update(info.condition, -1);
				edited = true;
			} else if (RandomUtil.WhetherToHappend(Constant.GetConstant().training_injury_probability)) {
				AddInjury(player, RandomUtil.SampleFromUniformDistribution(
						1, Constant.GetConstant().max_training_injury_length));
			} else if (kConditionAccessor.Get(info.condition) < 100) {
				info.condition = kConditionAccessor.Update(info.condition, 3);
				edited = true;
			}
			if (kStaminaAccessor.Get(info.stamina) < 100) {
				info.stamina = kStaminaAccessor.Update(info.stamina, Math.max(30, (40 - player.GetAge()) * 4));
				edited = true;
			}
			if (edited) player.WriteInfo();
		}
	}
	
	public void UpdateStamina(PlayerData player, int stamina) {
		player.GetInfo().stamina = kStaminaAccessor.Set(stamina);
		player.WriteInfo();
	}
	
	public void AddInjury(PlayerData player, int injury_length) {
		player.GetInfo().injury = Integer.valueOf(injury_length);
		player.WriteInfo();
		player.AddInjuryRecord(injury_length);
	}
	
	private static FieldAccessor kConditionAccessor = new FieldAccessor(100, 50, 100);
	private static FieldAccessor kStaminaAccessor = new FieldAccessor(100, 0, 100);
}
