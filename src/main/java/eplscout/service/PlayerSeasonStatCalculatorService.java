package eplscout.service;

import eplscout.dao.PlayerSeasonStatDao;
import eplscout.db.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * 시즌 집계 계산기
 *
 * - player_match_stat 기준으로 시즌 누적 스탯 계산
 * - player_id + season 기준으로 player_season_stat UPDATE
 */
public class PlayerSeasonStatCalculatorService {

    private final PlayerSeasonStatDao seasonStatDao =
            new PlayerSeasonStatDao();

    public void calculateSeasonStats(int season) {

        String sql = """
            SELECT
                player_id,
                season,
                COUNT(*) AS appearances,
                SUM(minutes_played) AS total_minutes,
                AVG(rating) AS avg_rating
            FROM player_match_stat
            WHERE season = ?
            GROUP BY player_id, season
        """;

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, season);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    long playerId = rs.getLong("player_id");
                    int appearances = rs.getInt("appearances");
                    int totalMinutes = rs.getInt("total_minutes");

                    Double avgRating = rs.getDouble("avg_rating");
                    if (rs.wasNull()) {
                        avgRating = null;
                    }

                    // DAO 시그니처와 정확히 일치
                    seasonStatDao.updateSeasonAggregate(
                            playerId,
                            season,
                            appearances,
                            totalMinutes,
                            avgRating
                    );
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(
                "시즌 누적 스탯 계산 실패 (season=" + season + ")",
                e
            );
        }
    }
}
