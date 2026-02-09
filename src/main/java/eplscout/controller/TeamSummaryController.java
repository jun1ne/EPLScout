package eplscout.controller;

import eplscout.service.TeamSummaryService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/team")
public class TeamSummaryController {

    private final TeamSummaryService service = new TeamSummaryService();

    @GetMapping("/summary/{teamId}/{season}")
    public Map<String, Object> getTeamSummary(
            @PathVariable int teamId,
            @PathVariable int season
    ) {
        return service.getTeamSummary(teamId, season);
    }
}
