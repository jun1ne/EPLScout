package eplscout.controller;

import eplscout.dao.TeamDao;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/team")
public class TeamController {

    private final TeamDao teamDao = new TeamDao();

    /**
     * ===============================
     * 팀 목록 조회 (select box 용)
     * GET /api/team/list?season=2023
     * ===============================
     */
    @GetMapping("/list")
    public List<Map<String, Object>> getTeamList(
            @RequestParam int season
    ) {
        return teamDao.findTeamsForView(season);
    }
}
