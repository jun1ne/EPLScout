package eplscout.dao;

import eplscout.db.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

import org.springframework.stereotype.Repository;

/**
 * TeamPositionStatDao
 *
 * 역할:
 * - 팀 포지션별 선수 수 조회
 * - 팀 포지션 부족 분석
 * - 팀 포지션별 평균 평점 조회
 *
 * 사용처:
 * - 팀 관리 화면 (포지션 분포 + 평균 평점)
 * - 스카우트 추천 근거 설명
 * - 메인 대시보드 (포지션 부족 알림)
 */
@Repository
public class TeamPositionStatDao {

    /* =====================================================
       1. 팀 포지션별 선수 수 조회 (기존)
       ===================================================== */

    public Map<String, Integer> getPositionCounts(int teamId, int season) {

        String sql = """
            SELECT
                position,
                player_count
            FROM team_position_stat
            WHERE team_id = ?
              AND season = ?
        """;

        Map<String, Integer> result = new HashMap<>();

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, teamId);
            ps.setInt(2, season);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(
                        rs.getString("position"),
                        rs.getInt("player_count")
                    );
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(
                "팀 포지션별 선수 수 조회 실패", e
            );
        }

        return result;
    }

    /* =====================================================
       2. 팀 포지션별 평균 평점 조회 
       ===================================================== */

    /**
     * 팀 내 포지션별 평균 평점 조회
     *
     * 사용 목적:
     * - 어떤 포지션의 경기력이 가장 낮은지 설명
     * - 스카우트 추천이 특정 포지션으로 쏠리는 이유 제공
     *
     * @return { position -> avg_rating }
     */
    public Map<String, Double> getPositionAvgRatings(
            int teamId,
            int season
    ) {

        String sql = """
            SELECT
                p.position,
                AVG(pss.avg_rating) AS avg_rating
            FROM player_season_stat pss
            JOIN player p ON pss.player_id = p.player_id
            WHERE pss.team_id = ?
              AND pss.season = ?
            GROUP BY p.position
        """;

        Map<String, Double> result = new HashMap<>();

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, teamId);
            ps.setInt(2, season);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(
                        rs.getString("position"),
                        rs.getDouble("avg_rating")
                    );
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(
                "팀 포지션별 평균 평점 조회 실패", e
            );
        }

        return result;
    }

    /* =====================================================
       3. 대시보드용: 포지션 부족 팀 요약 (기존)
       ===================================================== */

    /**
     * 포지션별 최소 기준 미달 팀 조회
     *
     * 기준 (룰 기반):
     * - GK < 2
     * - DF < 6
     * - MF < 6
     * - FW < 4
     *
     * @return [{team, position, count}]
     */
    public List<Map<String, Object>> findTeamsWithWeakPositions(int season) {

        String sql = """
            SELECT
                t.name AS team_name,
                tps.position,
                tps.player_count
            FROM team_position_stat tps
            JOIN team t ON tps.team_id = t.team_id
            WHERE tps.season = ?
              AND (
                    (tps.position = 'GK' AND tps.player_count < 2) OR
                    (tps.position = 'DF' AND tps.player_count < 6) OR
                    (tps.position = 'MF' AND tps.player_count < 6) OR
                    (tps.position = 'FW' AND tps.player_count < 4)
              )
            ORDER BY t.name
        """;

        List<Map<String, Object>> list = new ArrayList<>();

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, season);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {

                    Map<String, Object> row = new HashMap<>();
                    row.put("team", rs.getString("team_name"));
                    row.put("position", rs.getString("position"));
                    row.put("count", rs.getInt("player_count"));

                    list.add(row);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(
                "포지션 부족 팀 조회 실패", e
            );
        }

        return list;
    }
}
