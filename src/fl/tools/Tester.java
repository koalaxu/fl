package fl.tools;

import java.util.ArrayList;
import java.util.List;

import fl.data.utils.FileUtil;
import fl.db.DataBase;
import fl.engine.data.DataAccessor;
import fl.utils.QuantileCalculator;
import fl.utils.RandomUtil;

public class Tester {
	public static void main(String[] args) {
		System.err.println(FileUtil.GetAbsolutePath());
		FileUtil.Init("/Users/koalaxu/Documents/Backup/Projects/fl/data/");
		DataBase db = new DataBase("test/");
		db.Load();
		DataAccessor accessor = DataAccessor.CreateDataAccessor("test/");
//		for (PlayerData player : accessor.GetAllActivePlayers()) {
//			if (player.GetInfo().position == null) {
//				player.GetInfo().position = Position.SecondStriker.name;
//			} else if (player.GetInfo().position != Position.GoalKeeper.name &&
//					player.GetInfo().secondary_position == null) {
//				player.GetInfo().secondary_position = Position.SecondStriker.name;
//			}
//			player.WriteInfo();
//		}
		accessor.Close();
//		System.err.println(StringEscapeUtils.escapeHtml4("ı"));
//		System.err.println("ı".length());
//		System.err.println(StringEscapeUtils.escapeHtml3("ı"));
//		System.err.println("ı".getBytes().length);
//		System.err.println("i".getBytes().length);
//		ObjectOutputStream out = new ObjectOutputStream(fos);
//		Integer size = array.size();
//		out.writeObject(size);
//		for (T t : array) {
//			out.writeObject(t);
//		}
//		OutputStream os = System.err;
//		try {
//			os.write("ı".getBytes());
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		double x = 0;
//		double y = 0;
//		for (int i = 0; i < 1000000; ++i) {
//			Vector offset = new Vector(RandomUtil.SampleFromUniformDistribution(0, Math.PI * 2));
//			x += offset.x;
//			y += offset.y;
//		}
//		System.err.println(x / 1000000);
//		System.err.println(y / 1000000);
//		LeagueRecordData data = accessor.GetLeague(3).GetLeagueTable(1900);
//		data.ComputeRank();
//		data.WriteHistory();
//		accessor.Close();
//		List<ClubData> teams = data.GetRankedTeams();
//		for (int i = 0; i < teams.size(); ++i) {
//			System.err.println(i + " - " + teams.get(i).GetData().name);
//		}
//		ComponentHub ch = new ComponentHub(accessor);
//		double red = 0;
//		double yellow = 0;
//		double home_goal = 0;
//		double away_goal = 0;
//		double x0 = 0;
//		double x1 = 0;
//		double y = 0;
//		int num_match = 100;
//		QuantileCalculator cal0 = new QuantileCalculator(QuantileCalculator.kNormalPercentiles, num_match);
//		QuantileCalculator cal1 = new QuantileCalculator(QuantileCalculator.kNormalPercentiles, num_match);
//		QuantileCalculator cal2 = new QuantileCalculator(QuantileCalculator.kNormalPercentiles, num_match);
//		QuantileCalculator cal3 = new QuantileCalculator(QuantileCalculator.kNormalPercentiles, num_match);
//		for (int i = 0; i < num_match; ++i) {
//			Match match = new Match();
//			match.league_id = 1;
//			int league = RandomUtil.SampleFromUniformDistribution(0, 5);
//			match.home_club = RandomUtil.SampleFromUniformDistribution(1, 18) + league * 36;
//			match.away_club = RandomUtil.SampleFromUniformDistribution(1, 18) + league * 36;
//			match.knockout = false;
//			MatchData match_data = accessor.CreateOneMatch(match);
//			ch.match_simulator.ProceedOneMatch(match_data, null, new StringLogger());
//			//ch.match_simulator.ProceedOneMatch(match_data, null, new PrintLogger());
//			System.err.println(match_data.GetResult().home.stats.goals + ":" +
//					match_data.GetResult().away.stats.goals);
//			MatchStats home_stats = match_data.GetHomeStats();
//			MatchStats away_stats = match_data.GetAwayStats();
////			System.err.println(home_stats.yellow + "-" + home_stats.red + " : " +
////			away_stats.yellow + "-" + away_stats.red);
//			red += home_stats.red + away_stats.red;
//			yellow += home_stats.yellow + away_stats.yellow;
//			home_goal += match_data.GetResult().home.stats.goals;
//			away_goal += match_data.GetResult().away.stats.goals;
//			x0 += home_stats.shot;
//			x1 += away_stats.shot;
//			y += home_stats.shot_ontarget + away_stats.shot_ontarget;
//			cal0.AddOneElement(home_stats.shot);
//			cal1.AddOneElement(away_stats.shot);
//			cal2.AddOneElement(home_stats.shot_ontarget);
//			cal3.AddOneElement(away_stats.shot_ontarget);
////			y += home_stats.shot_ontarget + away_stats.shot_ontarget;
////			System.err.println(home_stats.shot + " : " + away_stats.shot + "  -  "
////					+ (match.home_club - 1) % 18 + " : " + (match.away_club - 1) % 18);
////			System.err.println(match_data.GetResult().home.stats.control_time + " : " +
////					match_data.GetResult().away.stats.control_time);
////			x += match_data.GetResult().home.stats.penalty_kick + match_data.GetResult().away.stats.penalty_kick;
////			x += match_data.GetResult().home.stats.goals + match_data.GetResult().away.stats.goals;
////			for (Event event : match_data.GetResult().home.events) {
////				if (event.type == EventType.SUBSTITUTE) {
////					x++;
////				}
////			}
//		}
//		System.err.println(red / 2 / num_match);
//		System.err.println(yellow / 2 / num_match);
//		System.err.println(home_goal / num_match);
//		System.err.println(away_goal / num_match);
//		System.err.println(x0 / num_match);
//		System.err.println(x1 / num_match);
//		System.err.println(y / (x0 + x1));
//		double[] values = cal0.Compute();
//		for (int i = 0; i <  values.length; ++i) {
//			System.err.print(cal0.GetQuantileOrder()[i] + " = " + values[i] + ", ");
//		}
//		System.err.println();
//		values = cal1.Compute();
//		for (int i = 0; i <  values.length; ++i) {
//			System.err.print(cal0.GetQuantileOrder()[i] + " = " + values[i] + ", ");
//		}
//		System.err.println();
//		values = cal2.Compute();
//		for (int i = 0; i <  values.length; ++i) {
//			System.err.print(cal0.GetQuantileOrder()[i] + " = " + values[i] + ", ");
//		}
//		System.err.println();
//		values = cal3.Compute();
//		for (int i = 0; i <  values.length; ++i) {
//			System.err.print(cal0.GetQuantileOrder()[i] + " = " + values[i] + ", ");
//		}
//		System.err.println();
//		System.err.println(JsonUtil.WriteOneObjectToJson(match_data.GetResult().home.lineup));
//		System.err.println(JsonUtil.WriteOneObjectToJson(match_data.GetResult().away.lineup));
//		System.err.println(JsonUtil.WriteOneObjectToJson(match_data.GetResult().home.events));
//		System.err.println(JsonUtil.WriteOneObjectToJson(match_data.GetResult().away.events));
//		System.err.println(JsonUtil.WriteOneObjectToJson(match_data.GetResult().home.stats));
//		System.err.println(JsonUtil.WriteOneObjectToJson(match_data.GetResult().away.stats));
//		System.err.println(JsonUtil.WriteOneObjectToJson(match_data.GetHomeStats()));
//		System.err.println(JsonUtil.WriteOneObjectToJson(match_data.GetAwayStats()));
//		System.err.println(PitchUtil.GetShotAngle(new Point(112, 18), 1));
//		System.err.println(PitchUtil.GetShotVerticalAngle(100));
//		System.err.println(PitchUtil.GetShotVerticalAngle(64));
//		System.err.println(PitchUtil.GetShotVerticalAngle(36));
//		System.err.println(GameUtil.ShotOnTargetProbability(0.6435, 36, 20, false));
//		System.err.println(GameUtil.ShotOnTargetProbability(0.6435, 36, 15, false));
//		System.err.println(GameUtil.ShotOnTargetProbability(0.6435, 36, 10, false));
//		System.err.println(GameUtil.ShotOnTargetProbability(0.3707, 64, 20, false));
//		System.err.println(GameUtil.ShotOnTargetProbability(0.3707, 64, 15, false));
//		System.err.println(GameUtil.ShotOnTargetProbability(0.3707, 64, 10, false));
//		System.err.println(GameUtil.ShotOnTargetProbability(0.2388, 100, 20, false));
//		System.err.println(GameUtil.ShotOnTargetProbability(0.2388, 100, 15, false));
//		System.err.println(GameUtil.ShotOnTargetProbability(0.2388, 100, 10, false));	
//		System.err.println(GameUtil.ShotOnTargetProbability(0.6435, 36, 20, true));
//		System.err.println(GameUtil.ShotOnTargetProbability(0.6435, 36, 15, true));
//		System.err.println(GameUtil.ShotOnTargetProbability(0.6435, 36, 10, true));
//		System.err.println(GameUtil.ShotOnTargetProbability(1.176, 18, 20, true));
//		System.err.println(GameUtil.ShotOnTargetProbability(1.176, 18, 15, true));
//		System.err.println(GameUtil.ShotOnTargetProbability(1.176, 18, 10, true));
//		Vector v = new Vector(4, 3);
//		Vector u = GeometryUtil.Rotate(v, 0.3);
//		System.err.println(u);
//		System.err.println(GeometryUtil.Angle(v, u));
//		u = GeometryUtil.Rotate(v, -0.3);
//		System.err.println(GeometryUtil.Angle(v, u));
//		System.err.println(u);
		
//		LineSegment l0 = GeometryUtil.CreateLineSegment(new Point(0, 0), new Point(224, 0));
//		LineSegment l1 = GeometryUtil.CreateLineSegment(new Point(86.716912,209.644411), new Point(103.855049,-0.615190));
//		System.err.println(GeometryUtil.Intersect(l0, l1));
//		WageModel model = new WageModel();
//		TreeMap<PositionName, List<Double>> counts = new TreeMap<PositionName, List<Double>>();
//		List<Double> club_stats = new ArrayList<Double>();
//		for (PositionName pos : Position.positions.keySet()) {
//			counts.put(pos, new ArrayList<Double>());
//		}
//		for (ClubData club : accessor.GetAllClubs()) {
//			TreeSet<Double> top = new TreeSet<Double>();
//			for (PlayerData player_data : club.GetPlayers()) {
//
//			PlayerInfo player = player_data.GetInfo();
//			Position position = player.GetPosition();
//			double conf = PositionAnalyzer.GetPositionConfidence(player, position.type);
//			Ability.Type[] types = position.type == PositionType.GOAL_KEEPER ?
//					Ability.kGkAbilties : Ability.kNoGkAbilties;
//			for (Ability.Type type : types) {
//				conf += 0.1 * player.ability.GetAbility(type) / (Ability.kMaxAbility * types.length);
//			}
//			//counts.get(position.name).add(conf * 20);
//			top.add(conf * 20);
//			if (player.club_info != null)
//			counts.get(position.name).add((double)player.club_info.wage);
//			model.AddOnePlayerToModel(player);
//			}
//			double total_conf = 0;
//			for (int i = 0; i < 11; ++i) {
//				total_conf += top.pollLast();
//			}
//			club_stats.add(total_conf / 11);
//		}
//		double[] desired_quantiles = {0, 0.01, 0.05, 0.25, 0.5, 0.75, 0.95, 0.99, 1};
//		for (PositionName pos : Position.positions.keySet()) {
//			double[] ret = QuantileCalculator.Compute(desired_quantiles, counts.get(pos));
//			System.err.print(pos + " : "  + " (");
//			for (double d : ret) {
//				System.err.print((int)d + " ");
//			}
//			System.err.println(")");
//		}
//		
//		
//		double[] ret = QuantileCalculator.Compute(desired_quantiles, club_stats);
//		System.err.println(club_stats.size());
//		System.err.print("club : "  + " (");
//		for (double d : ret) {
//			System.err.print((int)d + " ");
//		}
//		System.err.println(")");
//		
//		model.Learn();
		
		
//		Map<PositionName, Integer> counts = new TreeMap<PositionName, Integer>();
//		InitializationSetup setup =
//				JsonUtil.ParseOneObjectFromJson("constants/initialization_setup.json",
//						InitializationSetup.class);
//		double wsum = 0;
//		for (Entry<FormationName, Double> e :setup.formation_popularity.entrySet()) {
//			List<Position> pos_list = FormationUtil.GetFormationPositions(Formation.formations.get(e.getKey()));
//			for (Position pos : pos_list) {
//				counts.put(pos.name, (int) (counts.getOrDefault(pos.name, 0) + e.getValue() * 1000));
//			}
//			wsum += pos_list.size();
//			if (pos_list.size() != 11) System.err.println(e.getKey());
//		}
//		System.err.println(wsum);
//		List<PlayerInfo> candidates = YouthUtil.GenerateCandidates(false, 100000);
//		Map<PositionName, Integer> counts2 = new TreeMap<PositionName, Integer>();
//		for (PlayerInfo candidate : candidates) {
//			List<Position> recommended_pos = PositionAnalyzer.GetReccommendPositions(candidate);
//			PositionName pos = recommended_pos.get(0).name;
//			candidate.position = pos;
//			counts.put(pos, counts.getOrDefault(pos, 0) + 1);
//			if (recommended_pos.size() > 1) {
//				pos = recommended_pos.get(1).name;
//				counts2.put(pos, counts2.getOrDefault(pos, 0) + 1);
//			}
//		}
//		double sum = 0;
//		for (Entry<PositionName, Integer> e : counts.entrySet()) {
//			System.err.println(e.getKey()+ " = " + (double)e.getValue() / 1000);
//			sum += (double)e.getValue() / 1000;
//		}
//		System.err.println(sum);
//		for (Entry<PositionName, Integer> e : counts2.entrySet()) {
//			System.err.println(e.getKey()+ " = " + (double)e.getValue() / 1000);
//			sum += (double)e.getValue() / 1000;
//		}
//		Map<FormationName, Integer> counts = new TreeMap<FormationName, Integer>();
//		for (ClubData club : accessor.GetAllClubs()) {
//			counts.put(club.GetInfo().favorite_formation,
//					counts.getOrDefault(club.GetInfo().favorite_formation, 0) + 1);
//		}
//		for (Entry<FormationName, Integer> e : counts.entrySet()) {
//		System.err.println(e.getKey()+ " = " + (double)e.getValue() / (12 * 36));
//		}
//		
//		MultiParameterModel model = new MultiParameterModel(2);
//		model.AddData(4.2, 1, 1);
//		model.AddData(12.1, 2, 2);
//		model.AddData(9.9, 1, 2);
//		model.Process();
//		for (double d : model.betas) { System.err.println(d); }
//	}
		List<Double> e = new ArrayList<Double>();
		for (int i = 0; i < 100000; ++i) {
			double a = RandomUtil.SampleFromNormalDistribution(9, 3.5);
			for (int j = 16; j < 26; ++j) {
				if (j < 26) {
					a += RandomUtil.WhetherToHappend(0.15) ? 1 : 0;
				} else {
					a += RandomUtil.WhetherToHappend(0.1) ? 1 : (RandomUtil.WhetherToHappend(0.1 / 0.9) ? -1 : 0);
				}
			}
			e.add(a);
		}
		double[] desired_quantiles = {0, 0.0025, 0.01, 0.05, 0.25, 0.5, 0.75, 0.95, 0.99, 0.9975, 1};
		double[] ret = QuantileCalculator.Compute(desired_quantiles, e);
		System.err.println(e.size());
		System.err.print(""  + " (");
		for (double d : ret) {
			System.err.print((int)d + " ");
		}
		System.err.println(")");
	}
}
