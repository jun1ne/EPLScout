package eplscout.controller;

import eplscout.service.LeagueStandingService;
import org.springframework.web.bind.annotation.*;

/**
 * LeagueStandingController
 *
 * 역할
 * - 메인보드(대시보드)에서 사용할 리그 순위표 API 제공
 * - (임시) 리그 순위 API → DB 적재 트리거 제공
 *
 * 설계 원칙
 * - 조회 로직과 적재 로직 분리
 * - 추천/스카우트 로직과 완전히 독립
 */
@RestController
@RequestMapping("/api/league")
public class LeagueStandingController {

    private final LeagueStandingService leagueStandingService;

    public LeagueStandingController(
            LeagueStandingService leagueStandingService
    ) {
        this.leagueStandingService = leagueStandingService;
    }

    /* ==================================================
       리그 순위표 조회 (메인보드용)
       ================================================== */

    /**
     * 리그 순위표 조회 API
     *
     * 예시 요청
     * GET /api/league/standings?leagueId=39&season=2023
     *
     * 반환 데이터
     * [
     *   {
     *     rank: 1,
     *     teamId: 1,
     *     teamName: "Arsenal",
     *     played: 38,
     *     win: 26,
     *     draw: 6,
     *     lose: 6,
     *     points: 84,
     *     goalsFor: 88,
     *     goalsAgainst: 43,
     *     goalDiff: 45
     *   },
     *   ...
     * ]
     */
    @GetMapping("/standings")
    public Object getLeagueStandings(
            @RequestParam int leagueId,
            @RequestParam int season
    ) {
        return leagueStandingService.getStandingsForView(
                leagueId,
                season
        );
    }

    /* ==================================================
       리그 순위 API → DB 적재 (임시 트리거)
       ================================================== */

    /**
     * 리그 순위 데이터를 외부 API에서 가져와 DB에 적재
     *
     * 
     * - 개발/시연용 임시 엔드포인트
     * - 실제 서비스에서는 스케줄러/배치로 대체
     *
     * 호출 예시
     * GET /api/league/standings/load
     */
    @GetMapping("/standings/load")
    public String loadLeagueStandings() throws Exception {

        // EPL (leagueId=39), 2023 시즌 기준 적재
        leagueStandingService.fetchAndSaveStandings(39, 2023);

        return "League standings loaded successfully";
    }
}
