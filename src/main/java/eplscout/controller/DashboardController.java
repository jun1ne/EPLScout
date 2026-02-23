package eplscout.controller;

import eplscout.service.DashboardService;
import eplscout.service.LeagueNewsService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService =
            new DashboardService();

    private final LeagueNewsService leagueNewsService =
            new LeagueNewsService();

    /**
     * ===============================
     * 메인 대시보드 요약
     * GET /api/dashboard?season=2023
     * ===============================
     */
    @GetMapping
    public Map<String, Object> getDashboard(
            @RequestParam int season
    ) {
        return dashboardService.getDashboardSummary(season);
    }

    /**
     * ===============================
     * 리그 뉴스 API
     * GET /api/dashboard/news
     * ===============================
     */
    @GetMapping("/news")
    public List<Map<String, Object>> getLeagueNews() {

        int leagueId = 39; // EPL

        List<Map<String, Object>> news =
                leagueNewsService.getLeagueNews(leagueId);

        // API 실패 시 Mock fallback
        if (news == null || news.isEmpty()) {

            news = List.of(
                    Map.of(
                            "title", "Arsenal title race intensifies",
                            "description", "Premier League title race heating up",
                            "image", "",
                            "url", "#",
                            "source", "Mock"
                    ),
                    Map.of(
                            "title", "Manchester City tactical shift explained",
                            "description", "Pep adjusts formation mid-season",
                            "image", "",
                            "url", "#",
                            "source", "Mock"
                    )
            );
        }

        return news;
    }
}
