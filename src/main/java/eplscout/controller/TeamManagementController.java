package eplscout.controller;

import eplscout.service.TeamManagementService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * TeamManagementController
 *
 * 역할:
 * - 팀 관리 화면용 상세 정보 제공
 */
@RestController
@RequestMapping("/api/team")
public class TeamManagementController {

    private final TeamManagementService service =
            new TeamManagementService();

    /**
     * 팀 상세 정보 조회
     *
     * GET /api/team/management/{teamId}/{season}
     */
    @GetMapping("/{teamId}/{season}")
    public Map<String, Object> getTeamDetail(
            @PathVariable int teamId,
            @PathVariable int season
    ) {
        return service.getTeamDetail(teamId, season);
    }
}
