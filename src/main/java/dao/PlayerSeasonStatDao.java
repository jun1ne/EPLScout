package dao;

import db.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * PlayerSeasonStatDao
 *
 * - 선수 시즌 누적 스탯 저장 전담 DAO
 * - /players API 결과를 그대로 시즌 단위로 적재
 *
 * 설계 포인트
 * - (player_id, season) UNIQUE 기준 UPSERT
 * - 추천 점수 계산의 "기본 재료"
 */
public class PlayerSeasonStatDao {

    /**
     * API 기반 시즌 누적 스탯 UPSERT
     * (PlayerSeasonStatService 에서 사용)
     */
    public void upsert(
            long playerId,
            long teamId,
            int season,
            int appearances,
            int minutesPlayed,
            Double rating
    ) {

        String sql = """
            INSERT INTO player_season_stat
                (player_id, team_id, season, appearances, minutes_played, avg_rating)
            VALUES
                (?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                appearances = VALUES(appearances),
                minutes_played = VALUES(minutes_played),
                avg_rating = VALUES(avg_rating)
        """;

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, playerId);
            ps.setLong(2, teamId);
            ps.setInt(3, season);
            ps.setInt(4, appearances);
            ps.setInt(5, minutesPlayed);

            if (rating != null) {
                ps.setDouble(6, rating);
            } else {
                ps.setNull(6, java.sql.Types.DECIMAL);
            }

            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException(
                "player_season_stat UPSERT 실패 (playerId=" + playerId + ")",
                e
            );
        }
    }

    /**
     * match_stat → 시즌 집계 UPDATE
     * (PlayerSeasonStatCalculatorService 에서 사용)
     */
    public void updateSeasonAggregate(
            long playerId,
            int season,
            int appearances,
            int totalMinutes,
            Double avgRating
    ) {

        String sql = """
            UPDATE player_season_stat
            SET
                appearances = ?,
                minutes_played = ?,
                avg_rating = ?
            WHERE player_id = ?
              AND season = ?
        """;

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, appearances);
            ps.setInt(2, totalMinutes);

            if (avgRating != null) {
                ps.setDouble(3, avgRating);
            } else {
                ps.setNull(3, java.sql.Types.DECIMAL);
            }

            ps.setLong(4, playerId);
            ps.setInt(5, season);

            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException(
                "player_season_stat 집계 UPDATE 실패 (playerId=" + playerId + ")",
                e
            );
        }
    }
}
