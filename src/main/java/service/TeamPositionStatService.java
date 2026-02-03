package service;

import db.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class TeamPositionStatService {

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
}
