package eplscout.service;

import eplscout.db.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.springframework.stereotype.Service;

/**
 * TeamPositionStatService
 *
 * 역할 요약:
 * 1) 팀의 포지션별 시즌 스탯을 분석 (기존 기능)
 * 2) 포지션 우선순위 계산을 위한 "보조 계산 로직" 제공 (추가)
 *
 * - 이 클래스는 "의사결정"을 하지 않는다
 * - 팀 상태를 분석하고 수치로 제공하는 역할만 담당
 * - 실제 추천 판단은 ScoutRecommendationService에서 수행
 */


@Service
public class TeamPositionStatService {

    /**
     * [기존 기능]
     * 팀별 / 포지션별 시즌 평균 스탯 분석
     *
     * - 디버깅 및 로그 출력용
     * - 추천 로직에는 직접 사용되지 않음
     */
    public void analyzeTeamPositionStats(int season) {

        String sql = """
            SELECT
                pss.team_id,
                p.position,
                COUNT(*) AS player_count,
                AVG(pss.appearances) AS avg_appearances,
                AVG(pss.minutes_played) AS avg_minutes,
                AVG(pss.avg_rating) AS avg_rating
            FROM player_season_stat pss
            JOIN player p
              ON pss.player_id = p.player_id
            WHERE pss.season = ?
            GROUP BY pss.team_id, p.position
            ORDER BY pss.team_id, p.position
        """;

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, season);

            try (ResultSet rs = ps.executeQuery()) {

                System.out.println("===== 팀 포지션별 시즌 스탯 =====");

                while (rs.next()) {
                    long teamId = rs.getLong("team_id");
                    String position = rs.getString("position");

                    double avgApps = rs.getDouble("avg_appearances");
                    double avgMinutes = rs.getDouble("avg_minutes");
                    double avgRating = rs.getDouble("avg_rating");

                    System.out.printf(
                        "team=%d | pos=%s | apps=%.1f | min=%.0f | rating=%.2f%n",
                        teamId, position, avgApps, avgMinutes, avgRating
                    );
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("팀 포지션별 스탯 분석 실패", e);
        }
    }

    // ==================================================
    // [ADD] 포지션 우선순위 계산 메서드
    // ==================================================

    /**
     * 포지션 우선순위 점수 계산
     *
     * 계산 기준:
     * - 인원 부족 가중치 : 0.7
     * - 평점 약점 가중치 : 0.3
     *
     * 사용 목적:
     * - ScoutRecommendationService에서
     *   선수 최종 점수에 곱해지는 "포지션 가중치" 산출
     *
     *  이 메서드는 판단을 내리지 않는다
     * - 단순히 "팀 상태를 수치화"하는 역할만 수행
     *
     * @param currentCount     현재 팀 내 해당 포지션 인원
     * @param requiredCount    설계 기준상 적정 인원
     * @param avgRating        팀 내 해당 포지션 평균 평점
     * @param leagueAvgRating  리그 평균 평점 기준값
     * @return                 포지션 우선순위 점수 (0.0 ~ 1.0)
     */
    public double calculatePositionPriority(
            int currentCount,
            int requiredCount,
            double avgRating,
            double leagueAvgRating
    ) {
        // --------------------------
        // 1 인원 부족 점수 계산
        // --------------------------
        double shortageScore = 0.0;

        if (currentCount < requiredCount) {
            shortageScore =
                    (double) (requiredCount - currentCount) / requiredCount;
        }

        // --------------------------
        // 2 평점 약점 점수 계산
        // --------------------------
        double ratingWeaknessScore = 0.0;

        if (avgRating < leagueAvgRating) {
            ratingWeaknessScore =
                    (leagueAvgRating - avgRating) / leagueAvgRating;
        }

        // --------------------------
        // 3 가중 합산 (설계 핵심)
        // --------------------------
        return shortageScore * 0.7
             + ratingWeaknessScore * 0.3;
    }
}
