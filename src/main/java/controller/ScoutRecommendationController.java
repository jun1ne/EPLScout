package controller;

import org.springframework.web.bind.annotation.*;
import service.ScoutRecommendationService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recommend")
public class ScoutRecommendationController {

    private final ScoutRecommendationService service =
            new ScoutRecommendationService();

    /**
     *  추천 배치 실행 (계산 + DB 저장)
     * POST /api/recommend/run?teamId=33&season=2023
     */
    @GetMapping("/run")
    public String run(
            @RequestParam int teamId,
            @RequestParam int season
    ) {
        service.recommendPlayers(teamId, season);
        return "OK";
    }

    /**
     *  추천 결과 조회 (화면용)
     * GET /api/recommend/results?teamId=33&season=2023
     */
    @GetMapping("/results")
    public List<Map<String, Object>> results(
            @RequestParam int teamId,
            @RequestParam int season
    ) {
        return service.getRecommendations(teamId, season);
    }

    /**
     * 추천 사유 단건 조회 (선수 클릭용)
     * GET /api/recommend/reason?teamId=33&playerId=101&season=2023
     */
    @GetMapping("/reason")
    public String reason(
            @RequestParam long teamId,
            @RequestParam long playerId,
            @RequestParam int season
    ) {
        return service.getRecommendationReason(teamId, playerId, season);
    }
}
