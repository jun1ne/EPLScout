package eplscout.dao;

import eplscout.db.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.springframework.stereotype.Repository;

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
@Repository
public class PlayerSeasonStatDao {

    /**
     * ============================================
     * 기존 UPSERT (절대 수정 x)
     * - 현재 배치 / 추천 로직에서 사용 중
     * ============================================
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
                (player_id, team_id, season,
                 appearances, minutes_played, avg_rating)
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
     * ==========================================================
     * [ADD] 확장 스탯 포함 UPSERT
     * - 포지션별 보정 점수 계산을 위한 데이터 적재
     * - 기존 upsert를 대체하지 않고 "병행" 사용
     * ==========================================================
     */
    public void upsertWithExtendedStats(
            long playerId,
            long teamId,
            int season,

            // ---------- base ----------
            int appearances,
            int minutesPlayed,
            Double rating,

            // ---------- attack ----------
            int goals,
            int assists,
            int shots,

            // ---------- pass ----------
            int keyPasses,
            double passAccuracy,

            // ---------- defense ----------
            int tackles,
            int interceptions,
            int clearances,

            // ---------- goalkeeper ----------
            int saves,
            int goalsConceded
    ) {

        String sql = """
            INSERT INTO player_season_stat
                (player_id, team_id, season,
                 appearances, minutes_played, avg_rating,

                 goals, assists, shots,
                 key_passes, pass_accuracy,
                 tackles, interceptions, clearances,
                 saves, goals_conceded)
            VALUES
                (?, ?, ?, ?, ?, ?,
                 ?, ?, ?,
                 ?, ?,
                 ?, ?, ?,
                 ?, ?)
            ON DUPLICATE KEY UPDATE
                appearances      = VALUES(appearances),
                minutes_played   = VALUES(minutes_played),
                avg_rating       = VALUES(avg_rating),

                goals            = VALUES(goals),
                assists          = VALUES(assists),
                shots            = VALUES(shots),
                key_passes       = VALUES(key_passes),
                pass_accuracy    = VALUES(pass_accuracy),
                tackles          = VALUES(tackles),
                interceptions    = VALUES(interceptions),
                clearances       = VALUES(clearances),
                saves            = VALUES(saves),
                goals_conceded   = VALUES(goals_conceded)
        """;

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int idx = 1;

            // ---------- PK / FK ----------
            ps.setLong(idx++, playerId);
            ps.setLong(idx++, teamId);
            ps.setInt(idx++, season);

            // ---------- base ----------
            ps.setInt(idx++, appearances);
            ps.setInt(idx++, minutesPlayed);

            if (rating != null) {
                ps.setDouble(idx++, rating);
            } else {
                ps.setNull(idx++, java.sql.Types.DECIMAL);
            }

            // ---------- attack ----------
            ps.setInt(idx++, goals);
            ps.setInt(idx++, assists);
            ps.setInt(idx++, shots);

            // ---------- pass ----------
            ps.setInt(idx++, keyPasses);
            ps.setDouble(idx++, passAccuracy);

            // ---------- defense ----------
            ps.setInt(idx++, tackles);
            ps.setInt(idx++, interceptions);
            ps.setInt(idx++, clearances);

            // ---------- goalkeeper ----------
            ps.setInt(idx++, saves);
            ps.setInt(idx++, goalsConceded);

            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException(
                "player_season_stat 확장 UPSERT 실패 (playerId=" + playerId + ")",
                e
            );
        }
    }

    /**
     * ==========================================================
     * 기존 시즌 집계 UPDATE (절대 수정 x)
     * ==========================================================
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
