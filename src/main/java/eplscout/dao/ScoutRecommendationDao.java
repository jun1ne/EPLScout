package eplscout.dao;

import eplscout.db.DBUtil;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

@Repository
public class ScoutRecommendationDao {

    /* ===============================
       추천 결과 UPSERT
       =============================== */
    public void upsert(
            long teamId,
            long playerId,
            int season,
            String position,
            double score,
            double potentialScore,
            String reason
    ) {

        String sql = """
            INSERT INTO scout_recommendation
                (team_id, player_id, season, position,
                 score, potential_score, reason)
            VALUES
                (?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                score = VALUES(score),
                potential_score = VALUES(potential_score),
                reason = VALUES(reason),
                created_at = CURRENT_TIMESTAMP
        """;

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, teamId);
            ps.setLong(2, playerId);
            ps.setInt(3, season);
            ps.setString(4, position); // DF / MF / FW / GK
            ps.setDouble(5, score);
            ps.setDouble(6, potentialScore);
            ps.setString(7, reason);

            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException("추천 결과 저장 실패", e);
        }
    }

    /* ===============================
       프론트 완전 호환 조회
       =============================== */
    public List<Map<String, Object>> findForView(long teamId, int season) {

        String sql = """
            SELECT
                p.player_id,
                p.name,
                p.age,
                sr.position,
                sr.score,
                sr.potential_score
            FROM scout_recommendation sr
            JOIN player p ON sr.player_id = p.player_id
            WHERE sr.team_id = ?
              AND sr.season = ?
            ORDER BY sr.score DESC
            LIMIT 10
        """;

        List<Map<String, Object>> list = new ArrayList<>();

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, teamId);
            ps.setInt(2, season);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {

                    Map<String, Object> row = new HashMap<>();

                    double score = rs.getDouble("score");

                    row.put("playerId", rs.getLong("player_id"));
                    row.put("playerName", rs.getString("name"));
                    row.put("age", rs.getInt("age"));

                    // 이미 DF / MF / FW / GK 형태 → 그대로 전달
                    row.put("position", rs.getString("position"));

                    row.put("score", score);
                    row.put("potentialScore", rs.getDouble("potential_score"));

                    // UI 설명용 breakdown (프론트에서 조건부 표시)
                    row.put("baseScore", score * 0.8);
                    row.put("positionBonus", score * 0.2);

                    list.add(row);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("추천 조회 실패", e);
        }

        return list;
    }
}
