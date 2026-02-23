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

        System.out.println(" FINAL VERSION LOADED");

        Map<String, Object> result = new HashMap<>();

        try (Connection conn = DBUtil.getConnection()) {

            // =========================
            // 팀 이름 + 로고 조회 (수정)
            // =========================
            String teamSql = """
                SELECT name, logo_url
                FROM team
                WHERE team_id = ?
            """;

            try (PreparedStatement psTeam = conn.prepareStatement(teamSql)) {
                psTeam.setLong(1, teamId);
                try (ResultSet rs = psTeam.executeQuery()) {
                    if (rs.next()) {
                        result.put("teamName", rs.getString("name"));
                        result.put("teamLogo", rs.getString("logo_url"));
                    }
                }
            }

            // =========================
            // 팀 시즌 전체 스탯 집계 (추가)
            // =========================
            String statSql = """
                SELECT
                    SUM(appearances) AS games_played,
                    SUM(goals) AS total_goals,
                    SUM(goals_conceded) AS goals_conceded
                FROM player_season_stat
                WHERE team_id = ?
                  AND season = ?
            """;

            try (PreparedStatement psStat = conn.prepareStatement(statSql)) {

                psStat.setLong(1, teamId);
                psStat.setInt(2, season);

                try (ResultSet rs = psStat.executeQuery()) {
                    if (rs.next()) {
                        result.put("gamesPlayed", rs.getInt("games_played"));
                        result.put("totalGoals", rs.getInt("total_goals"));
                        result.put("goalsConceded", rs.getInt("goals_conceded"));
                    }
                }
            }

            String sql = """
                SELECT
                    p.position,
                    COUNT(*) AS cnt,
                    AVG(pss.avg_rating) AS avg_rating
                FROM player_season_stat pss
                JOIN player p ON pss.player_id = p.player_id
                WHERE pss.team_id = ?
                  AND pss.season = ?
                  AND pss.avg_rating IS NOT NULL
                GROUP BY p.position
            """;

            Map<String, Integer> positionCounts = new HashMap<>();
            Map<String, Double> positionAvgRatings = new HashMap<>();

            int totalCount = 0;
            double ratingSum = 0.0;

            try (PreparedStatement ps = conn.prepareStatement(sql)) {

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
            }

            // =========================
            // 평균 정보
            // =========================
            double avgAge = calculateAvgAge(conn, teamId, season);
            double avgRating = totalCount == 0 ? 0.0 : ratingSum / totalCount;

            result.put("avgAge", avgAge);
            result.put("avgRating", avgRating);
            result.put("teamAvgRating", avgRating);
            result.put("positionCounts", positionCounts);
            result.put("positionAvgRatings", positionAvgRatings);

            // =========================
            // 인원 기준 부족 포지션
            // =========================
            Map<String, Integer> minRequired = Map.of(
                    "Goalkeeper", 2,
                    "Defender", 8,
                    "Midfielder", 8,
                    "Attacker", 6
            );

            Set<String> weakPositions = new HashSet<>();

            for (Map.Entry<String, Integer> entry : minRequired.entrySet()) {

                String position = entry.getKey();
                int required = entry.getValue();
                int current = positionCounts.getOrDefault(position, 0);

                if (current < required) {
                    weakPositions.add(position);
                }
            }

            // =========================
            // 평점 기준 약점 포지션
            // GK 완전 제외
            // 최소 3명 이상일 때만 평가
            // 팀 평균 대비 gap 가장 큰 포지션 1개 선택
            // gap이 0.15 이상일 때만 약점
            // =========================
            String weakestByRating = null;
            double largestGap = 0;

            for (Map.Entry<String, Double> entry : positionAvgRatings.entrySet()) {

                String position = entry.getKey();

                if ("Goalkeeper".equals(position)) continue;

                int count = positionCounts.getOrDefault(position, 0);
                if (count < 3) continue;

                double posAvg = entry.getValue();
                double gap = avgRating - posAvg;

                if (gap > largestGap) {
                    largestGap = gap;
                    weakestByRating = position;
                }
            }

            if (largestGap >= 0.15) {
                weakPositions.add(weakestByRating);
            } else {
                weakestByRating = null;
            }

            result.put("weakestPositionByRating", weakestByRating);
            result.put("weakPositions", new ArrayList<>(weakPositions));

            // =========================
            // 디버깅 로그
            // =========================
            System.out.println("==== TEAM SUMMARY ====");
            System.out.println("TEAM: " + teamId + " SEASON: " + season);
            System.out.println("TEAM AVG RATING: " + avgRating);
            System.out.println("POSITION COUNTS: " + positionCounts);
            System.out.println("POSITION AVG RATINGS: " + positionAvgRatings);
            System.out.println("WEAKEST BY RATING: " + weakestByRating);
            System.out.println("WEAK POSITIONS FINAL: " + weakPositions);
            System.out.println("======================");

        } catch (Exception e) {
            throw new RuntimeException("팀 요약 조회 실패", e);
        }

        return result;
    }

    /**
     * 팀 평균 연령 계산
     */
    private double calculateAvgAge(Connection conn, long teamId, int season) {

        String sql = """
            SELECT AVG(p.age)
            FROM player_season_stat pss
            JOIN player p ON pss.player_id = p.player_id
            WHERE pss.team_id = ?
              AND pss.season = ?
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

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
