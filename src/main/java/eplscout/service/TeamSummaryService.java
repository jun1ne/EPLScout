package eplscout.service;

import eplscout.db.DBUtil;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;

@Service
public class TeamSummaryService {

    /**
     * 팀 요약 정보 조회
     *
     * - 포지션별 인원 수
     * - 포지션별 평균 평점
     * - 팀 평균 연령
     * - 평점 기준 약점 포지션
     * - 인원 기준 부족 포지션
     */
    public Map<String, Object> getTeamSummary(long teamId, int season) {

        Map<String, Object> result = new HashMap<>();

        String sql = """
            SELECT
                p.position,
                COUNT(*) AS cnt,
                AVG(pss.avg_rating) AS avg_rating
            FROM player_season_stat pss
            JOIN player p ON pss.player_id = p.player_id
            WHERE pss.team_id = ?
              AND pss.season = ?
            GROUP BY p.position
        """;

        Map<String, Integer> positionCounts = new HashMap<>();
        Map<String, Double> positionAvgRatings = new HashMap<>();

        int totalCount = 0;
        double ratingSum = 0.0;

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

          
            ps.setLong(1, teamId);
            ps.setInt(2, season);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {

                    String position = rs.getString("position");
                    int count = rs.getInt("cnt");
                    double avgRating = rs.getDouble("avg_rating");

                    positionCounts.put(position, count);
                    positionAvgRatings.put(position, avgRating);

                    totalCount += count;
                    ratingSum += avgRating * count;
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("팀 포지션 요약 조회 실패", e);
        }

        // =========================
        // 평균 정보
        // =========================
        double avgAge = calculateAvgAge(teamId, season);
        double avgRating = totalCount == 0 ? 0.0 : ratingSum / totalCount;

        result.put("avgAge", avgAge);
        result.put("avgRating", avgRating);
        result.put("positionCounts", positionCounts);
        result.put("positionAvgRatings", positionAvgRatings);

        // =========================
        // 평점 기준 약점 포지션
        // =========================
        String weakestByRating =
                positionAvgRatings.entrySet().stream()
                        .min(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse(null);

        result.put("weakestPositionByRating", weakestByRating);

        // =========================
        // 인원 기준 부족 포지션
        // =========================
        Map<String, Integer> minRequired = Map.of(
                "Goalkeeper", 2,
                "Defender", 6,
                "Midfielder", 6,
                "Attacker", 4
        );

        List<String> weakPositions = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : minRequired.entrySet()) {
            String position = entry.getKey();
            int required = entry.getValue();
            int current = positionCounts.getOrDefault(position, 0);

            if (current < required) {
                weakPositions.add(position);
            }
        }

        result.put("weakPositions", weakPositions);

        return result;
    }

    /**
     * 팀 평균 연령 계산
     */
    private double calculateAvgAge(long teamId, int season) {

        String sql = """
            SELECT AVG(p.age)
            FROM player_season_stat pss
            JOIN player p ON pss.player_id = p.player_id
            WHERE pss.team_id = ?
              AND pss.season = ?
        """;

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

           
            ps.setLong(1, teamId);
            ps.setInt(2, season);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("팀 평균 연령 계산 실패", e);
        }

        return 0.0;
    }
}
