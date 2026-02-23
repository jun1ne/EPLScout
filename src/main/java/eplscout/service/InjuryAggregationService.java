package eplscout.service;

import eplscout.db.DBUtil;

import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * InjuryAggregationService
 *
 * 역할:
 * 1. player_injury_history 기반
 * 2. 최근 3년 부상 통계 집계
 * 3. player_injury_stat 테이블 업데이트
 *
 * 추천 계산 전 반드시 실행
 */
@Service
public class InjuryAggregationService {

    /**
     * 최근 3년 기준 집계
     *
     * @param currentSeason 현재 시즌 (예: 2025)
     */
    public void aggregateLast3Years(int currentSeason) {

        int season1 = currentSeason;
        int season2 = currentSeason - 1;
        int season3 = currentSeason - 2;

        String sql = """
            SELECT
                player_id,
                COUNT(*) AS total_injury,
                COUNT(CASE WHEN season = ? THEN 1 END) AS current_season_injury
            FROM player_injury_history
            WHERE season IN (?, ?, ?)
            GROUP BY player_id
        """;

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, season1);
            ps.setInt(2, season1);
            ps.setInt(3, season2);
            ps.setInt(4, season3);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    long playerId = rs.getLong("player_id");
                    int totalInjury = rs.getInt("total_injury");
                    int currentSeasonInjury =
                            rs.getInt("current_season_injury");

                    // 결장률 계산 (38경기 기준)
                    double injuryRate =
                            currentSeasonInjury / 38.0;

                    // 반복 부상 계산
                    int repeatCount =
                            calculateRepeatInjury(conn, playerId);

                    // 현재 부상 여부는 나중에 API에서 업데이트
                    int currentInjured = 0;

                    upsertInjuryStat(
                            conn,
                            playerId,
                            season1,
                            currentSeasonInjury,
                            injuryRate,
                            repeatCount,
                            totalInjury,
                            currentInjured
                    );
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(
                    "부상 3년 집계 실패", e);
        }
    }

    /**
     * 동일 injury_type 반복 횟수 계산
     */
    private int calculateRepeatInjury(Connection conn, long playerId)
            throws Exception {

        String sql = """
            SELECT injury_type, COUNT(*) AS cnt
            FROM player_injury_history
            WHERE player_id = ?
            GROUP BY injury_type
            HAVING COUNT(*) >= 2
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, playerId);

            try (ResultSet rs = ps.executeQuery()) {
                int repeat = 0;

                while (rs.next()) {
                    repeat += rs.getInt("cnt");
                }

                return repeat;
            }
        }
    }

    /**
     * player_injury_stat UPSERT
     */
    private void upsertInjuryStat(
            Connection conn,
            long playerId,
            int season,
            int injuryGames,
            double injuryRate,
            int repeatCount,
            int totalLast3Years,
            int currentInjured
    ) throws Exception {

        String sql = """
            INSERT INTO player_injury_stat
            (player_id, season, injury_games,
             injury_rate, repeat_injury_count,
             total_injury_last3years, current_injured)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                injury_games = VALUES(injury_games),
                injury_rate = VALUES(injury_rate),
                repeat_injury_count = VALUES(repeat_injury_count),
                total_injury_last3years = VALUES(total_injury_last3years),
                current_injured = VALUES(current_injured)
        """;

        try (PreparedStatement ps =
                     conn.prepareStatement(sql)) {

            ps.setLong(1, playerId);
            ps.setInt(2, season);
            ps.setInt(3, injuryGames);
            ps.setDouble(4, injuryRate);
            ps.setInt(5, repeatCount);
            ps.setInt(6, totalLast3Years);
            ps.setInt(7, currentInjured);

            ps.executeUpdate();
        }
    }
}
