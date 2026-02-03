package service;

import dao.TeamDao;
import java.util.List;

public class ScoutBatchService {

    public void runFullBatch() throws Exception {

        final int LEAGUE_ID = 39;
        final int SEASON = 2023;
        final int TARGET_TEAM_ID = 1;

        TeamDao teamDao = new TeamDao();
        PlayerSeasonStatService playerSeasonStatService =
                new PlayerSeasonStatService();
        PlayerSeasonStatCalculatorService calculatorService =
                new PlayerSeasonStatCalculatorService();
        TeamPositionStatService teamPositionStatService =
                new TeamPositionStatService();
        ScoutRecommendationService scoutService =
                new ScoutRecommendationService();

        System.out.println("EPL 팀 목록 조회");
        List<Integer> apiTeamIds =
                teamDao.findApiTeamIdsBySeason(SEASON);

        for (int apiTeamId : apiTeamIds) {
            playerSeasonStatService.loadTeamSeasonStats(
                    LEAGUE_ID,
                    SEASON,
                    apiTeamId
            );
        }

        calculatorService.calculateSeasonStats(SEASON);
        teamPositionStatService.analyzeTeamPositionStats(SEASON);
        scoutService.recommendPlayers(TARGET_TEAM_ID, SEASON);
        scoutService.showComparisonTop(TARGET_TEAM_ID, SEASON, 5);
    }
}
