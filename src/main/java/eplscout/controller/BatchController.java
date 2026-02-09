package eplscout.controller;

import eplscout.service.PlayerSeasonStatService;
import eplscout.service.ScoutRecommendationService;
import eplscout.dao.TeamDao;
import eplscout.model.Team;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/batch")
public class BatchController {

    private final PlayerSeasonStatService statService;
    private final ScoutRecommendationService recommendationService;
    private final TeamDao teamDao;

    public BatchController(
            PlayerSeasonStatService statService,
            ScoutRecommendationService recommendationService,
            TeamDao teamDao
    ) {
        this.statService = statService;
        this.recommendationService = recommendationService;
        this.teamDao = teamDao;
    }

    /**
     * 선수 시즌 스탯 적재 (리그 전체)
     * 호출:
     * /api/batch/player-stats?leagueId=39&season=2023
     */
    @GetMapping("/player-stats")
    public String loadPlayerStats(
            @RequestParam int leagueId,
            @RequestParam int season
    ) throws Exception {

        List<Team> teams =
                teamDao.findTeamsByLeagueAndSeason(leagueId, season);

        for (Team team : teams) {
            statService.loadTeamSeasonStats(
                    leagueId,
                    season,
                    team.getApiTeamId() 
            );
        }

        return "OK - player_season_stat 적재 완료";
    }

    /**
     * 단일 팀 추천 계산
     * 호출:
     * /api/batch/recommend?teamId=49&season=2023
     *  teamId = API 팀 ID
     */
    @GetMapping("/recommend")
    public String recommendOne(
            @RequestParam int teamId,
            @RequestParam int season
    ) throws Exception {

        long internalTeamId =
                teamDao.findTeamIdByApiTeamIdAndSeason(teamId, season);

        if (internalTeamId == 0) {
            return "ERROR - 팀 ID 매핑 실패";
        }

        recommendationService.recommendPlayers(
                internalTeamId,
                season
        );

        return "OK - 단일 팀 추천 완료";
    }

    /**
     * 모든 팀 추천 계산
     * 호출:
     * /api/batch/recommend-all?leagueId=39&season=2023
     */
    @GetMapping("/recommend-all")
public String recommendAll(
        @RequestParam int leagueId,
        @RequestParam int season
) {

    List<Team> teams =
            teamDao.findTeamsByLeagueAndSeason(leagueId, season);

    for (Team team : teams) {

        long internalTeamId = team.getTeamId(); //  내부 team_id

        System.out.println(
                "▶ 추천 배치 실행: teamId=" + internalTeamId
        );

        recommendationService.recommendPlayers(
                internalTeamId,
                season
        );
    }

    return "OK - 모든 팀 추천 완료";
}
}

