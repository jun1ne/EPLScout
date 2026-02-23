package eplscout.dao;

import eplscout.db.DBUtil;
import eplscout.model.PlayerInjuryStat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * PlayerInjuryStatDao
 *
 *  player_injury_stat 테이블 접근 전용 DAO
 *  Service는 직접 JDBC를 사용하지 않음 (계층 분리)
 */
public class PlayerInjuryStatDao {

    /**
     * 시즌 기준 부상 통계 조회
     */
    public PlayerInjuryStat findByPlayerAndSeason(long playerId, int season) {

        String sql = """
            SELECT *
            FROM player_injury_stat
            WHERE player_id = ?
              AND season = ?
        """;

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, playerId);
            ps.setInt(2, season);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {

                    PlayerInjuryStat stat = new PlayerInjuryStat();

                    stat.setPlayerId(playerId);
                    stat.setSeason(season);
                    stat.setInjuryGames(rs.getInt("injury_games"));
                    stat.setInjuryRate(rs.getDouble("injury_rate"));
                    stat.setRepeatInjuryCount(rs.getInt("repeat_injury_count"));
                    stat.setTotalInjuryLast3Years(
                            rs.getInt("total_injury_last3years"));
                    stat.setCurrentInjured(
                            rs.getInt("current_injured") == 1);

                    return stat;
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("부상 통계 조회 실패", e);
        }

        return null;
    }
}
