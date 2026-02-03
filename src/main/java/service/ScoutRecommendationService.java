package service;

import dao.ScoutRecommendationDao;
import db.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.List;
import java.util.Map;


/**
 * ScoutRecommendationService
 *
 * 역할 요약
 * - 팀 약점 포지션 분석
 * - 선수 시즌 스탯 기반 추천 점수 계산
 * - 즉시전력 / 포텐셜 점수 산출
 * - LLM을 활용한 "추천 사유 설명" 생성
 * - LLM 응답은 DB에 캐싱하여 비용/속도 최적화
 */
public class ScoutRecommendationService {

    private final ScoutRecommendationDao recommendationDao =
            new ScoutRecommendationDao();

    // LLM은 "설명 생성" 용도로만 사용
    private final LLMService llmService =
            new LLMService();

    /**
     * 추천 계산 + DB 저장
     */
    public void recommendPlayers(int teamId, int season) {

        String weakPosition = findWeakPosition(teamId, season);

        System.out.println("▶ 팀 약점 포지션: " + weakPosition);
        System.out.println("===== 추천 선수 계산 & 저장 =====");

        String sql = """
            SELECT
                p.player_id,
                p.name,
                p.position,
                p.age,
                pss.appearances,
                pss.minutes_played,
                pss.avg_rating
            FROM player_season_stat pss
            JOIN player p ON pss.player_id = p.player_id
            WHERE pss.season = ?
              AND pss.team_id != ?
        """;

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, season);
            ps.setInt(2, teamId);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    long playerId = rs.getLong("player_id");
                    String name = rs.getString("name");
                    String position = rs.getString("position");
                    int age = rs.getInt("age");

                    int apps = rs.getInt("appearances");
                    int minutes = rs.getInt("minutes_played");
                    double rating = rs.getDouble("avg_rating");

                    /* ===============================
                       1 즉시전력 점수 계산
                       =============================== */

                    double appearanceScore = apps * 0.4;
                    double minuteScore = (minutes / 90.0) * 0.3;
                    double ratingScore = rating * 10 * 0.3;

                    double baseScore =
                            appearanceScore + minuteScore + ratingScore;

                    boolean weakBoost =
                            weakPosition != null && position.equals(weakPosition);

                    double finalScore = weakBoost
                            ? baseScore * 1.3
                            : baseScore;

                    /* ===============================
                       2 포텐셜 점수 계산
                       =============================== */

                    double ageScore;
                    if (age <= 23) ageScore = 10;
                    else if (age <= 26) ageScore = 6;
                    else ageScore = 2;

                    double efficiencyScore =
                            minutes < 300
                                    ? 5
                                    : (rating * 10) / (minutes / 90.0 + 1);

                    efficiencyScore = Math.min(efficiencyScore, 15);

                    double potentialScore = ageScore + efficiencyScore;

                    /* ===============================
                       3 Rule-based 추천 사유
                       =============================== */

                    String ruleReason = String.format(
                            "즉시전력(%.1f) | 포텐셜[나이 %.1f + 효율 %.1f]",
                            finalScore,
                            ageScore,
                            efficiencyScore
                    );

                    /* ===============================
                       4 LLM 추천 사유 (캐싱 핵심)
                       =============================== */

                    // ① 이미 저장된 LLM 설명이 있는지 확인
                    String llmComment =
                            recommendationDao.findLlmComment(
                                    teamId, playerId, season
                            );

                    // ② 없을 때만 LLM 호출
                    if (llmComment == null || llmComment.isBlank()) {
                        try {
                            llmComment =
                                    llmService.generateRecommendationReason(
                                            "Target Team",
                                            name,
                                            position,
                                            finalScore,
                                            potentialScore,
                                            ruleReason
                                    );

                            // ③ LLM 응답 DB 캐싱
                            recommendationDao.updateLlmComment(
                                    teamId,
                                    playerId,
                                    season,
                                    llmComment
                            );

                        } catch (Exception e) {
                            // LLM 실패 시 rule-based 설명으로 대체
                            llmComment = ruleReason;
                        }
                    }

                    /* ===============================
                       5 추천 결과 UPSERT
                       =============================== */

                    recommendationDao.upsert(
                            teamId,
                            playerId,
                            season,
                            position,
                            finalScore,
                            potentialScore,
                            ruleReason
                    );

                    /* ===============================
                       6 콘솔 출력 (LLM 설명 사용)
                       =============================== */

                    System.out.printf(
                            "%s (%s, %d세)%n- score=%.2f | potential=%.2f%n- %s%n%n",
                            name,
                            position,
                            age,
                            finalScore,
                            potentialScore,
                            llmComment
                    );
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("추천 계산 실패", e);
        }
    }



    public void showTopRecommendations(long teamId, int season, int limit) {
        recommendationDao.findTopNByTeam(teamId, season, limit);
    }

    public void showComparisonTop(long teamId, int season, int limit) {
        System.out.println("즉시전력 TOP");
        recommendationDao.findTopNByTeamOrderBy(
                teamId, season, limit, "score"
        );

        System.out.println("포텐셜 TOP");
        recommendationDao.findTopNByTeamOrderBy(
                teamId, season, limit, "potential_score"
        );
    }

    private String findWeakPosition(int teamId, int season) {
        String sql = """
            SELECT
                p.position,
                AVG(pss.avg_rating) AS avg_rating
            FROM player_season_stat pss
            JOIN player p ON pss.player_id = p.player_id
            WHERE pss.team_id = ?
              AND pss.season = ?
            GROUP BY p.position
            ORDER BY avg_rating ASC
            LIMIT 1
        """;

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, teamId);
            ps.setInt(2, season);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("position");
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("팀 약점 포지션 조회 실패", e);
        }

        return null;
    }
        /**
     * GUI / API 조회 전용 메서드
     * - 추천 계산 X
     * - DB에 저장된 scout_recommendation 결과만 반환
     */
    public List<Map<String, Object>> getResults(
            long teamId,
            int season
    ) {
        return recommendationDao.findResults(teamId, season);
    }
    /**
 * GUI용: 추천 결과 리스트 조회
 */
public List<Map<String, Object>> getRecommendations(int teamId, int season) {
    return recommendationDao.findForView(teamId, season);
}

/**
 * GUI용: 추천 사유 단건 조회
 */
public String getRecommendationReason(long teamId, long playerId, int season) {
    String comment = recommendationDao.findLlmComment(teamId, playerId, season);
    return (comment != null && !comment.isBlank())
            ? comment
            : "추천 사유가 아직 생성되지 않았습니다.";
}


    

}
