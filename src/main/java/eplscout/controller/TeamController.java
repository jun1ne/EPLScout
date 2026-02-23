package eplscout.controller;

import eplscout.service.TeamSummaryService;
import eplscout.dao.TeamDao;
import eplscout.dao.PlayerDao;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/team")
public class TeamController {

    private final TeamDao teamDao;
    private final TeamSummaryService teamSummaryService;
    private final PlayerDao playerDao;  

    public TeamController(
            TeamDao teamDao,
            TeamSummaryService teamSummaryService,
            PlayerDao playerDao     
    ) {
        this.teamDao = teamDao;
        this.teamSummaryService = teamSummaryService;
        this.playerDao = playerDao;  
    }

    /* ===============================
       팀 목록 조회
    =============================== */
    @GetMapping("/list")
    public List<Map<String, Object>> getTeamList(
            @RequestParam int season
    ) {
        return teamDao.findTeamsForView(season);
    }

    /* ===============================
       팀 요약 조회
    =============================== */
    @GetMapping("/summary/{teamId}/{season}")
    public Map<String, Object> getTeamSummary(
            @PathVariable long teamId,
            @PathVariable int season
    ) {

        // =========================
        // 시즌 변경 시 내부 team_id 재매핑
        // =========================

        // 1. 현재 teamId의 api_team_id 조회
        int apiTeamId = teamDao.findApiTeamIdByTeamId(teamId);

        if (apiTeamId == 0) {
            throw new RuntimeException("api_team_id 조회 실패");
        }

        // 2. 해당 시즌의 team_id 재조회
        long seasonTeamId =
                teamDao.findTeamIdByApiTeamIdAndSeason(apiTeamId, season);

        if (seasonTeamId == 0L) {
            throw new RuntimeException("해당 시즌 team_id 없음");
        }

        // 3. 해당 시즌 team_id로 요약 실행
        return teamSummaryService.getTeamSummary(seasonTeamId, season);
    }

    /* ===============================
       팀 선수 목록 조회 
       - player + player_season_stat JOIN
       - 번호 / 국적 / 사진 포함
    =============================== */
    @GetMapping("/{teamId}/players")
    public List<Map<String, Object>> getPlayers(
            @PathVariable int teamId,
            @RequestParam int season
    ) {

        // =========================
        // 시즌 변경 시 내부 team_id 재매핑
        // =========================

        int apiTeamId = teamDao.findApiTeamIdByTeamId(teamId);

        if (apiTeamId == 0) {
            throw new RuntimeException("api_team_id 조회 실패");
        }

        long seasonTeamId =
                teamDao.findTeamIdByApiTeamIdAndSeason(apiTeamId, season);

        if (seasonTeamId == 0L) {
            return List.of();
        }

        return playerDao.getPlayersByTeamAndSeason((int) seasonTeamId, season);
    }
}
