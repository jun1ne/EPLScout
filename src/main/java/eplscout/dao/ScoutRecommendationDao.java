package eplscout.dao;

import eplscout.db.DBUtil;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ScoutRecommendationDao {

    /* ===============================
       추천 결과 UPSERT
       =============================== */
    public void upsert(
            Connection conn,
            long teamId,
            long playerId,
            int season,
            String position,
            double score,
            double potentialScore,
            double playerValue, 
            String reason
    ) {

        String sql = """
            INSERT INTO scout_recommendation
                (team_id, player_id, season, position,
                 score, potential_score, player_value, reason)
            VALUES
                (?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                score = VALUES(score),
                potential_score = VALUES(potential_score),
                player_value = VALUES(player_value),
                reason = VALUES(reason),
                created_at = CURRENT_TIMESTAMP
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, teamId);
            ps.setLong(2, playerId);
            ps.setInt(3, season);
            ps.setString(4, position);
            ps.setDouble(5, score);
            ps.setDouble(6, potentialScore);
            ps.setDouble(7, playerValue); 
            ps.setString(8, reason);

            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException("추천 결과 저장 실패", e);
        }
    }

    /* ===============================
       기존 추천 결과 삭제
       =============================== */
    public void deleteByTeamAndSeason(long teamId, int season) {

        String sql = """
            DELETE FROM scout_recommendation
            WHERE team_id = ?
              AND season = ?
        """;

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, teamId);
            ps.setInt(2, season);

            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException("추천 기존 데이터 삭제 실패", e);
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
                p.photo_url,
                sr.position,
                sr.score,
                sr.potential_score,
                sr.player_value, 
                t.logo_url AS club_logo
            FROM scout_recommendation sr
            JOIN player p ON sr.player_id = p.player_id
            JOIN player_season_stat ps 
                 ON ps.player_id = p.player_id
                AND ps.season = sr.season
            JOIN team t ON ps.team_id = t.team_id
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
                    row.put("position", rs.getString("position"));
                    row.put("score", score);
                    row.put("potentialScore", rs.getDouble("potential_score"));

                    //  선수 가치 추가
                    row.put("playerValue", rs.getDouble("player_value"));

               
                    row.put("photoUrl", rs.getString("photo_url"));
                    row.put("clubLogo", rs.getString("club_logo"));

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
