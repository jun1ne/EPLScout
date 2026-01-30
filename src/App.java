import service.PlayerSeasonStatService;
import service.PlayerSeasonStatCalculatorService;
import service.TeamPositionStatService;
import service.ScoutRecommendationService;
import dao.TeamDao;

import java.util.List;

/**
 * App (실행 진입점)
 *
 * 실행 흐름
 * 1. EPL 팀 목록 조회 (DB)
 * 2. 팀별 선수 시즌 누적 스탯 적재 (API → DB)
 * 3. 시즌 누적 스탯 집계 계산
 * 4. 팀 포지션별 시즌 스탯 분석
 * 5. 선수 추천 계산 (즉시전력 + 포텐셜 + LLM 설명)
 * 6. 즉시전력 TOP vs 포텐셜 TOP 비교 출력
 */
public class App {

    public static void main(String[] args) throws Exception {

        final int LEAGUE_ID = 39;      // EPL
        final int SEASON = 2023;
        final int TARGET_TEAM_ID = 1; // Arsenal 예시

        // DAO / Service
        TeamDao teamDao = new TeamDao();
        PlayerSeasonStatService playerSeasonStatService =
                new PlayerSeasonStatService();
        PlayerSeasonStatCalculatorService calculatorService =
                new PlayerSeasonStatCalculatorService();
        TeamPositionStatService teamPositionStatService =
                new TeamPositionStatService();
        ScoutRecommendationService scoutService =
                new ScoutRecommendationService();

        /* ===============================
           1. EPL 팀 목록 조회
           =============================== */

        System.out.println("=================================");
        System.out.println("EPL 팀 목록 조회 (DB 기준)");
        System.out.println("=================================");

        List<Integer> apiTeamIds =
                teamDao.findApiTeamIdsBySeason(SEASON);

        System.out.println("총 팀 수: " + apiTeamIds.size());
        System.out.println();

        /* ===============================
           2. 팀별 시즌 누적 스탯 적재
           =============================== */

        System.out.println("=================================");
        System.out.println("선수 시즌 누적 스탯 적재 시작");
        System.out.println("=================================");

        for (int apiTeamId : apiTeamIds) {
            System.out.println("▶ 팀 처리 중 (apiTeamId=" + apiTeamId + ")");
            playerSeasonStatService.loadTeamSeasonStats(
                    LEAGUE_ID,
                    SEASON,
                    apiTeamId
            );
        }

        System.out.println("=================================");
        System.out.println("시즌 누적 스탯 적재 완료");
        System.out.println("=================================");

        /* ===============================
           3. 시즌 누적 스탯 집계
           =============================== */

        System.out.println();
        System.out.println("=================================");
        System.out.println("시즌 누적 스탯 집계 계산");
        System.out.println("=================================");

        calculatorService.calculateSeasonStats(SEASON);

        /* ===============================
           4. 팀 포지션별 시즌 스탯 분석
           =============================== */

        System.out.println();
        System.out.println("=================================");
        System.out.println("팀 포지션별 시즌 스탯 분석");
        System.out.println("=================================");

        teamPositionStatService.analyzeTeamPositionStats(SEASON);

        /* ===============================
           5. 선수 추천 계산 (LLM 포함)
           =============================== */

        System.out.println();
        System.out.println("=================================");
        System.out.println("선수 추천 계산");
        System.out.println("=================================");

        scoutService.recommendPlayers(TARGET_TEAM_ID, SEASON);

        /* ===============================
           6. 즉시전력 vs 포텐셜 비교
           =============================== */

        System.out.println();
        System.out.println("=================================");
        System.out.println("즉시전력 vs 포텐셜 비교");
        System.out.println("=================================");

        scoutService.showComparisonTop(
                TARGET_TEAM_ID,
                SEASON,
                5
        );

        System.out.println();
        System.out.println("=================================");
        System.out.println("프로그램 종료");
        System.out.println("=================================");
    }
}
