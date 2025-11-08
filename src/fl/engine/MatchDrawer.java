package fl.engine;

import java.util.ArrayList
;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import fl.data.League;
import fl.data.League.Type;
import fl.data.LeagueInfo;
import fl.engine.data.ClubData;
import fl.engine.data.CountryData;
import fl.engine.data.DataAccessor;
import fl.engine.data.LeagueData;
import fl.engine.data.LeagueRecordData;
import fl.utils.RandomUtil;

public class MatchDrawer extends BaseComponent {

	protected MatchDrawer(DataAccessor data_accessor, ComponentHub component_hub) {
		super(data_accessor, component_hub);
	}
	
	public void ArrangeMatches() {
		for (LeagueData league : GetAccessor().GetAllLeauges()) {
			switch (league.GetData().type) {
			case DOMESTIC_LEAGUE:
				ShuffleTeams(league);
				break;
			case DOMESTIC_CUP:
				ShuffleKnockouts(league);
				break;
			case CONTINENTAL_CUP:
				DrawContinentalGroupStage(league);
				break;
			case DOMESTIC_SUPER_CUP:
			case CONTINENTAL_SUPER_CUP:
				DrawSuperCup(league);
				break;
			}
		}
	}
	
	public void ArrangeKnockoutStage() {
		for (LeagueData league : GetAccessor().GetAllLeauges()) {
			if (league.GetData().type != Type.CONTINENTAL_CUP) continue;
			DrawContinentalKnockoutStage(league);
		}
	}
	
	private void ShuffleTeams(LeagueData league) {
		ArrayList<Long> team_ids = new ArrayList<Long>();
		for (Long team_id : league.GetInfo().qualified_teams) {
			team_ids.add(team_id);
		}
		Collections.shuffle(team_ids);
		league.GetInfo().team_ids = team_ids;
		league.WriteInfo();
	}
	
	private void ShuffleKnockouts(LeagueData league) {
		ArrayList<Long> team_ids = new ArrayList<Long>();
		LeagueInfo league_info = league.GetInfo();
		for (int i = league_info.qualified_teams.size() - 1; i >= 0 ; --i) {
			team_ids.add(league_info.qualified_teams.get(i));
		}
		for (int i = 0; i < 4; ++i) team_ids.add(8, -1L);  // reserve 8, 9, 10, 11 for 1st round winners
		Collections.shuffle(team_ids.subList(0, 7));
		Collections.shuffle(team_ids.subList(12, 31));
		Collections.shuffle(team_ids.subList(32, 39));
		league.GetInfo().team_ids = team_ids;
		league.WriteInfo();
	}
	
	private void DrawContinentalGroupStage(LeagueData league) {
		ArrayList<ArrayList<ClubData>> seeded_teams = new ArrayList<ArrayList<ClubData>>();
		for (int i = 0; i < League.kTeamPerGroup; ++i) {
			ArrayList<ClubData> teams = new ArrayList<ClubData>();
			seeded_teams.add(teams);
			for (int j = 0; j < League.kGroupNumber; ++j) {
				teams.add(GetAccessor().GetClub(league.GetInfo().qualified_teams.get(i * League.kGroupNumber + j)));
			}
		}
		ClubData[][] selected_teams = new ClubData[League.kTeamPerGroup][League.kGroupNumber];
		List<HashSet<CountryData>> group_countries = new ArrayList<HashSet<CountryData>>();
		Collections.shuffle(seeded_teams.get(0));  // Shuffle seeds
		for (int i = 0; i < League.kTeamPerGroup; ++i) {
			if (i > 0) {
				DrawRemainingTeam(0, seeded_teams.get(i), selected_teams[i],
						gc -> !group_countries.get(gc.id).contains(gc.club.GetCountry()));
			}
			for (int j = 0; j < League.kGroupNumber; ++j) {
				if (i == 0) {
					group_countries.add(new HashSet<CountryData>());
					selected_teams[0][j] = seeded_teams.get(0).get(j);
				}
				group_countries.get(j).add(selected_teams[i][j].GetCountry());
			}
		}
		ArrayList<Long> team_ids = new ArrayList<Long>();
		for (int i = 0; i < League.kGroupNumber; ++i)
			for (int j = 0; j < League.kTeamPerGroup; ++j) {
				team_ids.add(selected_teams[j][i].GetKey());
			}
		league.GetInfo().team_ids = team_ids;
		league.WriteInfo();
	}
	
	private void DrawContinentalKnockoutStage(LeagueData league) {
		ArrayList<ClubData> first_place_teams = new ArrayList<ClubData>();
		ArrayList<ClubData> second_place_teams = new ArrayList<ClubData>();
		LeagueRecordData league_table = league.GetLeagueRecord(GetAccessor().GetGlobal().year);
		Map<ClubData, ClubData> group_pairs = new HashMap<ClubData, ClubData>();
		for (int i = 0; i < League.kGroupNumber; ++i) {
			ClubData first_place = league_table.GetGroupClubStats(i, 0).club;
			ClubData second_place = league_table.GetGroupClubStats(i, 1).club;
			first_place_teams.add(first_place);
			second_place_teams.add(second_place);
			group_pairs.put(second_place, first_place);
		}
		Collections.shuffle(first_place_teams);
		ClubData[] selected_teams = new ClubData[League.kGroupNumber];
		DrawRemainingTeam(0, second_place_teams, selected_teams,
				gc -> first_place_teams.get(gc.id) != group_pairs.get(gc.club) &&
					!first_place_teams.get(gc.id).GetCountry().equals(gc.club.GetCountry()));
		// Fill the match slots
		LeagueInfo info = league.GetInfo();
		for (int i = 0; i < League.kGroupNumber; ++i) {
			info.team_ids.add(first_place_teams.get(i).GetKey());
		}
		for (int i = 0; i < League.kGroupNumber; ++i) {
			info.team_ids.add(selected_teams[i].GetKey());
		}
		league.WriteInfo();
	}
	
	private void DrawSuperCup(LeagueData league) {
		league.GetInfo().team_ids = league.GetInfo().qualified_teams;
		league.WriteInfo();
	}
	
	private class GroupCandidate {
		public GroupCandidate(int id, ClubData club) {
			this.id = id;
			this.club = club;
		}
		public int id;
		public ClubData club;
	}
	
	private boolean DrawRemainingTeam(int id, List<ClubData> remaining_candidates, ClubData[] selection,
			Predicate<GroupCandidate> predicate) {
		List<ClubData> candidates = new ArrayList<ClubData>();
		candidates.addAll(remaining_candidates);
		do {
			ClubData candidate = 
					candidates.get(RandomUtil.SampleFromUniformDistribution(0, candidates.size() - 1));
			candidates.remove(candidate);
			if (predicate.test(new GroupCandidate(id, candidate))) {  // Check pass
				remaining_candidates.remove(candidate);
				if (remaining_candidates.isEmpty() ||
						DrawRemainingTeam(id + 1, remaining_candidates, selection, predicate)) {
					selection[id] = candidate;
					return true;
				} else { // No possible combination
					remaining_candidates.add(candidate);
				} 
			}
		} while (!candidates.isEmpty());
		return false;
	}
}
