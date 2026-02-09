package eplscout.controller;

import eplscout.service.DashboardService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService =
            new DashboardService();

    /**
     * 메인 대시보드 요약
     *
     * GET /api/dashboard?season=2023
     */
    @GetMapping
    public Map<String, Object> getDashboard(
            @RequestParam int season
    ) {
        return dashboardService.getDashboardSummary(season);
    }
}
