package eplscout.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Date;
import java.sql.Types;
import java.time.LocalDate;

import org.springframework.stereotype.Repository;

import eplscout.db.DBUtil;

/**
 * player_match_stat 전담 DAO
 * - 선수 경기 단위 원천 데이터 적재
 * - 추천/분석의 가장 밑바닥 데이터
 *
 * 설계 포인트
 * - (player_id, match_id) 기준 UPSERT
 * - 시즌/경기 단위 집계의 기반
 *
 * - DB 연결/SQL 예외는 DAO 내부에서 처리
 * - Service 계층에는 RuntimeException으로만 전달
 */
@Repository
public class PlayerMatchStatDao {

    public void upsert(
            long playerId,
            Long teamId,          // NULL 허용 (초기 적재 단계)
            long matchId,
            int season,
            LocalDate matchDate,
            int minutes,
            Double rating
    ) {

        String sql = """
            INSERT INTO player_match_stat
            (player_id, team_id, match_id, season, match_date, minutes_played, rating)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                minutes_played = VALUES(minutes_played),
                rating = VALUES(rating)
        """;

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // 1) 내부 player PK
            ps.setLong(1, playerId);

            // 2) team_id (초기 적재 단계에서는 NULL 허용)
            if (teamId != null) {
                ps.setLong(2, teamId);
            } else {
                ps.setNull(2, Types.BIGINT);
            }

            // 3) 경기 ID (API fixture.id)
            ps.setLong(3, matchId);

            // 4) 시즌
            ps.setInt(4, season);

            // 5) 경기 날짜
            ps.setDate(5, Date.valueOf(matchDate));

            // 6) 출전 시간
            ps.setInt(6, minutes);

            // 7) 평점 (NULL 가능)
            if (rating != null) {
                ps.setDouble(7, rating);
            } else {
                ps.setNull(7, Types.DECIMAL);
            }

            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException(
                "player_match_stat UPSERT 실패 " +
                "(playerId=" + playerId +
                ", matchId=" + matchId +
                ", season=" + season + ")",
                e
            );
        }
    }
}
