package eplscout.controller;

import eplscout.service.ScoutRecommendationService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/scout")
public class ScoutRecommendationController {

    private final ScoutRecommendationService scoutService;

    public ScoutRecommendationController(
            ScoutRecommendationService scoutService
    ) {
        this.scoutService = scoutService;
    }

    /**
     * 추천 결과 조회
     */
    @GetMapping("/view/{teamId}/{season}")
    public Map<String, Object> viewScout(
            @PathVariable long teamId,  
            @PathVariable int season
    ) {
        return scoutService.view(teamId, season);
    }

    /**
     * LLM 추천 사유 생성
     */
    @PostMapping("/recommendation/reason")
    public String generateRecommendationReason(
            @RequestBody Map<String, Object> body
    ) {

        String teamName = String.valueOf(body.get("teamName"));
        String playerName = String.valueOf(body.get("playerName"));
        String position = String.valueOf(body.get("position"));

        double score = parseDoubleSafe(body.get("score"));
        double potentialScore = parseDoubleSafe(body.get("potentialScore"));

        return scoutService.generateRecommendationReason(
                teamName,
                playerName,
                position,
                score,
                potentialScore
        );
    }

    // null-safe 파싱
    private double parseDoubleSafe(Object value) {
        if (value == null) return 0.0;
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception e) {
            return 0.0;
        }
    }
}
