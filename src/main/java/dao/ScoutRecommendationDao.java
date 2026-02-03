package dao;

import db.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ScoutRecommendationDao {

    /**
     * 추천 결과 UPSERT (포텐셜 점수 포함)
     * - rule-based reason만 저장
     * - llm_comment는 별도 메서드에서 관리
     */
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
                (team_id, player_id, season, position, score, potential_score, reason)
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
            ps.setString(4, position);
            ps.setDouble(5, score);
            ps.setDouble(6, potentialScore);
            ps.setString(7, reason);

            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException("추천 결과 저장 실패", e);
        }
    }

    /* =====================================================
       LLM 캐싱 관련 메서드
       ===================================================== */

    /**
     * 이미 저장된 LLM 추천 사유 조회
     *
     * @return llm_comment (없으면 null)
     */
    public String findLlmComment(
            long teamId,
            long playerId,
            int season
    ) {

        String sql = """
            SELECT llm_comment
            FROM scout_recommendation
            WHERE team_id = ?
              AND player_id = ?
              AND season = ?
        """;

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, teamId);
            ps.setLong(2, playerId);
            ps.setInt(3, season);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("llm_comment");
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("LLM 코멘트 조회 실패", e);
        }

        return null;
    }

    /**
     * LLM 추천 사유 저장 (최초 1회)
     */
    public void updateLlmComment(
            long teamId,
            long playerId,
            int season,
            String llmComment
    ) {

        String sql = """
            UPDATE scout_recommendation
            SET llm_comment = ?
            WHERE team_id = ?
              AND player_id = ?
              AND season = ?
        """;

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, llmComment);
            ps.setLong(2, teamId);
            ps.setLong(3, playerId);
            ps.setInt(4, season);

            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException("LLM 코멘트 저장 실패", e);
        }
    }

    /* =====================================================
       조회 메서드들 
       ===================================================== */

    public void findTopNByTeam(
            long teamId,
            int season,
            int limit
    ) {

        String sql = """
            SELECT
                sr.player_id,
                p.name,
                sr.position,
                sr.score,
                sr.potential_score,
                sr.reason
            FROM scout_recommendation sr
            JOIN player p
              ON sr.player_id = p.player_id
            WHERE sr.team_id = ?
              AND sr.season = ?
            ORDER BY sr.score DESC
            LIMIT ?
        """;

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, teamId);
            ps.setInt(2, season);
            ps.setInt(3, limit);

            try (ResultSet rs = ps.executeQuery()) {

                System.out.println("===== TOP " + limit + " 추천 선수 (즉시전력) =====");

                int rank = 1;
                while (rs.next()) {
                    String name = rs.getString("name");
                    String position = rs.getString("position");
                    double score = rs.getDouble("score");
                    double potential = rs.getDouble("potential_score");
                    String reason = rs.getString("reason");

                    System.out.printf(
                        "%d위. %s (%s) | score=%.2f | potential=%.2f | %s%n",
                        rank++, name, position, score, potential, reason
                    );
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("TOP 추천 조회 실패", e);
        }
    }

    /**
     * 정렬 기준 선택 조회
     */
    public void findTopNByTeamOrderBy(
            long teamId,
            int season,
            int limit,
            String orderBy
    ) {

        if (!"score".equals(orderBy) && !"potential_score".equals(orderBy)) {
            throw new IllegalArgumentException("허용되지 않은 정렬 기준: " + orderBy);
        }

        String sql =
            "SELECT " +
            "    p.name, " +
            "    sr.position, " +
            "    sr.score, " +
            "    sr.potential_score " +
            "FROM scout_recommendation sr " +
            "JOIN player p ON sr.player_id = p.player_id " +
            "WHERE sr.team_id = ? " +
            "  AND sr.season = ? " +
            "ORDER BY " + orderBy + " DESC " +
            "LIMIT ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, teamId);
            ps.setInt(2, season);
            ps.setInt(3, limit);

            try (ResultSet rs = ps.executeQuery()) {

                int rank = 1;
                while (rs.next()) {
                    String name = rs.getString("name");
                    String position = rs.getString("position");
                    double score = rs.getDouble("score");
                    double potential = rs.getDouble("potential_score");

                    System.out.printf(
                        "%d위. %s (%s) | score=%.2f | potential=%.2f%n",
                        rank++, name, position, score, potential
                    );
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("추천 TOP 조회 실패", e);
        }
    }
        /**
     * GUI / API 용 추천 결과 조회
     * - 계산 X
     * - SELECT 전용
     */
    public List<Map<String, Object>> findResults(
            long teamId,
            int season
    ) {

        String sql = """
            SELECT
                sr.recommendation_id,
                p.name,
                sr.position,
                sr.score,
                sr.potential_score,
                sr.llm_comment
            FROM scout_recommendation sr
            JOIN player p
            ON sr.player_id = p.player_id
            WHERE sr.team_id = ?
            AND sr.season = ?
            ORDER BY sr.score DESC
        """;

        List<Map<String, Object>> results = new ArrayList<>();

        try (Connection conn = DBUtil.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, teamId);
            ps.setInt(2, season);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {

                    Map<String, Object> row = new HashMap<>();
                    row.put("name", rs.getString("name"));
                    row.put("position", rs.getString("position"));
                    row.put("score", rs.getDouble("score"));
                    row.put("potential", rs.getDouble("potential_score"));
                    row.put("comment", rs.getString("llm_comment"));

                    results.add(row);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("추천 결과 조회 실패", e);
        }

        return results;
}
public List<Map<String, Object>> findTopResults(
        long teamId,
        int season,
        int limit
) {
    String sql = """
        SELECT
            p.name,
            sr.position,
            sr.score,
            sr.potential_score,
            sr.llm_comment
        FROM scout_recommendation sr
        JOIN player p ON sr.player_id = p.player_id
        WHERE sr.team_id = ?
          AND sr.season = ?
        ORDER BY sr.score DESC
        LIMIT ?
    """;

    List<Map<String, Object>> list = new ArrayList<>();

    try (Connection conn = DBUtil.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {

        ps.setLong(1, teamId);
        ps.setInt(2, season);
        ps.setInt(3, limit);

        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("name", rs.getString("name"));
                row.put("position", rs.getString("position"));
                row.put("score", rs.getDouble("score"));
                row.put("potential", rs.getDouble("potential_score"));
                row.put("comment", rs.getString("llm_comment"));
                list.add(row);
            }
        }
    } catch (Exception e) {
        throw new RuntimeException("추천 결과 조회 실패", e);
    }

    return list;
}
public List<Map<String, Object>> findForView(int teamId, int season) {

    String sql = """
        SELECT
            p.player_id,
            p.name,
            sr.position,
            sr.score,
            sr.potential_score,
            sr.llm_comment
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

        ps.setInt(1, teamId);
        ps.setInt(2, season);

        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("playerId", rs.getLong("player_id"));
                row.put("name", rs.getString("name"));
                row.put("position", rs.getString("position"));
                row.put("score", rs.getDouble("score"));
                row.put("potential", rs.getDouble("potential_score"));
                row.put("comment", rs.getString("llm_comment"));
                list.add(row);
            }
        }

    } catch (Exception e) {
        throw new RuntimeException(e);
    }

    return list;
}



}
