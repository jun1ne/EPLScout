package eplscout.controller;

import eplscout.dao.TeamDao;
import eplscout.model.Team;
import eplscout.service.*;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/batch")
public class BatchController {

    private final TeamApiService teamApiService;
    private final TeamDao teamDao;
    private final PlayerSeasonStatService statService;
    private final LeagueStandingService leagueStandingService;
    private final PlayerApiService playerApiService; 

    public BatchController(
            TeamApiService teamApiService,
            TeamDao teamDao,
            PlayerSeasonStatService statService,
            LeagueStandingService leagueStandingService,
            PlayerApiService playerApiService
    ) {
        this.teamApiService = teamApiService;
        this.teamDao = teamDao;
        this.statService = statService;
        this.leagueStandingService = leagueStandingService;
        this.playerApiService = playerApiService;
    }

    /* ==================================================
       전체 시즌 파이프라인 실행
    ================================================== */
    @GetMapping("/full")
    public String runFullPipeline(
            @RequestParam int leagueId,
            @RequestParam int start,
            @RequestParam int end
    ) throws Exception {

        for (int season = start; season <= end; season++) {

            System.out.println("==========");
            System.out.println("▶ 시즌 시작: " + season);
            System.out.println("==========");

            /*  리그 순위 */
            leagueStandingService.fetchAndSaveStandings(
                    leagueId,
                    season
            );

            /*  팀 수집 */
            List<Team> teams =
                    teamApiService.fetchTeams(leagueId, season);

            teamDao.upsertTeams(teams);

            /*  선수 기본정보 API 수집  */
            for (Team team : teams) {

                playerApiService.loadTeamPlayers(
                        team.getApiTeamId(),
                        season
                );

                statService.loadTeamSeasonStats(
                        leagueId,
                        season,
                        team.getApiTeamId()
                );
            }

            System.out.println(" 시즌 완료: " + season);
        }

        return "OK - 전체 시즌 파이프라인 완료";
    }
}
