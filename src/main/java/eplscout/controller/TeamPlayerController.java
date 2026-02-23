package eplscout.controller;

import eplscout.db.DBUtil;
import org.springframework.web.bind.annotation.*;

import java.sql.*;
import java.util.*;

@RestController
@RequestMapping("/api/player")
public class TeamPlayerController {

    /* ==================================================
       1. 팀 선수단 전체 조회
       ================================================== */

    /**
     * 팀 선수단 조회 (팀 관리 화면용)
     *
     * GET /api/team/{teamId}/players?season=2023
     */
    @GetMapping("/{teamId}/players")
    public List<Map<String, Object>> getTeamPlayers(
            @PathVariable int teamId,
            @RequestParam int season
    ) {

        String sql = """
            SELECT
                p.player_id,
                p.name,
                p.position,
                p.age,
                p.photo_url,      

                pss.appearances,
                pss.goals,
                pss.assists,
                pss.shots,
                pss.key_passes,
                pss.pass_accuracy,
                pss.tackles,
                pss.interceptions,
                pss.clearances,
                pss.saves,
                pss.goals_conceded,
                pss.avg_rating
            FROM player_season_stat pss
            JOIN player p ON p.player_id = pss.player_id
            WHERE pss.team_id = ?
              AND pss.season = ?
            ORDER BY p.position, p.name
        """;

        List<Map<String, Object>> list = new ArrayList<>();

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, teamId);
            ps.setInt(2, season);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapPlayerRow(rs));
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("팀 선수단 조회 실패", e);
        }

        return list;
    }

    /* ==================================================
       2. 선수 단일 상세 조회 
       ================================================== */

    /**
     * 선수 상세 조회 (차트 시각화용)
     *
     * GET /api/player/{playerId}?season=2023
     */
    @GetMapping("/{playerId}")
    public Map<String, Object> getPlayerDetail(
            @PathVariable long playerId,
            @RequestParam int season
    ) {

        String sql = """
            SELECT
                p.player_id,
                p.name,
                p.position,
                p.age,
                p.photo_url,        

                pss.appearances,
                pss.goals,
                pss.assists,
                pss.shots,
                pss.key_passes,
                pss.pass_accuracy,
                pss.tackles,
                pss.interceptions,
                pss.clearances,
                pss.saves,
                pss.goals_conceded,
                pss.avg_rating
            FROM player_season_stat pss
            JOIN player p ON p.player_id = pss.player_id
            WHERE p.player_id = ?
              AND pss.season = ?
        """;

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, playerId);
            ps.setInt(2, season);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapPlayerRow(rs);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("선수 상세 조회 실패", e);
        }

        throw new RuntimeException("선수를 찾을 수 없습니다. playerId=" + playerId);
    }

    /* ==================================================
       공통 ResultSet → Map 매핑
       ================================================== */

    /**
     * ResultSet → Player Map 변환
     *
     * - 팀 선수단 / 선수 단일 조회에서 공통 사용
     * - 프론트 차트 & 카드에 바로 사용 가능
     */
    private Map<String, Object> mapPlayerRow(ResultSet rs) throws SQLException {

        Map<String, Object> row = new HashMap<>();

        row.put("playerId", rs.getLong("player_id"));
        row.put("name", rs.getString("name"));
        row.put("position", rs.getString("position"));
        row.put("age", rs.getInt("age"));
        row.put("photoUrl", rs.getString("photo_url"));  // ✅ 추가

        row.put("appearances", rs.getInt("appearances"));
        row.put("goals", rs.getInt("goals"));
        row.put("assists", rs.getInt("assists"));
        row.put("shots", rs.getInt("shots"));
        row.put("keyPasses", rs.getInt("key_passes"));
        row.put("passAccuracy", rs.getDouble("pass_accuracy"));

        row.put("tackles", rs.getInt("tackles"));
        row.put("interceptions", rs.getInt("interceptions"));
        row.put("clearances", rs.getInt("clearances"));

        row.put("saves", rs.getInt("saves"));
        row.put("goalsConceded", rs.getInt("goals_conceded"));

        row.put("avgRating", rs.getDouble("avg_rating"));

        return row;
    }
    /* ==================================================
   3. 선수 시즌별 평점 추세 조회
   ================================================== */

/**
 * 선수 시즌별 평균 평점 추세
 *
 * GET /api/player/{playerId}/trend
 */
@GetMapping("/{playerId}/trend")
public List<Map<String, Object>> getPlayerTrend(
        @PathVariable long playerId
) {

    String sql = """
        SELECT season, avg_rating
        FROM player_season_stat
        WHERE player_id = ?
          AND avg_rating IS NOT NULL
        ORDER BY season
    """;

    List<Map<String, Object>> list = new ArrayList<>();

    try (Connection conn = DBUtil.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {

        ps.setLong(1, playerId);

        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {

                Map<String, Object> row = new HashMap<>();
                row.put("season", rs.getInt("season"));
                row.put("avgRating", rs.getDouble("avg_rating"));

                list.add(row);
            }
        }

    } catch (Exception e) {
        throw new RuntimeException("선수 평점 추세 조회 실패", e);
    }

    return list;
}
/* ==================================================
   4. 선수 가치 점수 계산
   ================================================== */

/**
 * 선수 가치 점수
 *
 * GET /api/player/{playerId}/value?season=2025
 */
@GetMapping("/{playerId}/value")
public Map<String, Object> getPlayerValue(
        @PathVariable long playerId,
        @RequestParam int season
) {

    String sql = """
        SELECT *
        FROM player_season_stat
        WHERE player_id = ?
          AND season = ?
    """;

    try (Connection conn = DBUtil.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {

        ps.setLong(1, playerId);
        ps.setInt(2, season);

        try (ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {

                double avgRating = rs.getDouble("avg_rating");
                int goals = rs.getInt("goals");
                int assists = rs.getInt("assists");
                int age =  rs.getInt("appearances") > 0 ? 0 : 0; // placeholder

                //  간단 계산식 
                double performance =
                        (avgRating * 10)
                        + (goals * 2)
                        + (assists * 1.5);

                double valueScore = Math.min(performance, 100);

                String grade =
                    valueScore >= 95 ? "Splus" :
                    valueScore >= 90 ? "S" :
                    valueScore >= 80 ? "A" :
                    valueScore >= 70 ? "B" :
                    "C";

                Map<String, Object> result = new HashMap<>();
                result.put("valueScore", valueScore);
                result.put("grade", grade);

                return result;
            }
        }

    } catch (Exception e) {
        throw new RuntimeException("선수 가치 계산 실패", e);
    }

    throw new RuntimeException("선수 가치 계산 실패");
}

/* ==================================================
   5. 리그 평균 비교
   ================================================== */

@GetMapping("/{playerId}/league-average")
public Map<String, Object> getLeagueAverage(
        @PathVariable long playerId,
        @RequestParam int season
) {

    String sql = """
        SELECT
            AVG(goals) as avg_goals,
            AVG(assists) as avg_assists,
            AVG(shots) as avg_shots,
            AVG(pass_accuracy) as avg_pass_accuracy
        FROM player_season_stat
        WHERE season = ?
    """;

    try (Connection conn = DBUtil.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {

        ps.setInt(1, season);

        try (ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {

                Map<String, Object> result = new HashMap<>();
                result.put("avgGoals", rs.getDouble("avg_goals"));
                result.put("avgAssists", rs.getDouble("avg_assists"));
                result.put("avgShots", rs.getDouble("avg_shots"));
                result.put("avgPassAccuracy", rs.getDouble("avg_pass_accuracy"));

                return result;
            }
        }

    } catch (Exception e) {
        throw new RuntimeException("리그 평균 조회 실패", e);
    }

    return Map.of();
}


}
