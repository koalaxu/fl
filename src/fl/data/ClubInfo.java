package fl.data;

import fl.data.Formation.FormationName;

public class ClubInfo {
	public long club_id;
	
	public Long fund;
	public Integer youth_training_level;
	public FormationName favorite_formation;
	
	static public int kMaxYouthTraininglevel = 100;
	public void SetYouthTrainingLevel(int level) {
		youth_training_level = Math.max(0, Math.min(kMaxYouthTraininglevel, level));
	}
	
	public int stadium_size;
	public long domestic_fans;
	public long international_fans;
	
	public transient double continental_score;
	
	public Formation GetFavoriteFormation() {
		return Formation.formations.get(favorite_formation);
	}
	public long estimated_income;
}
