package eplscout.dao;

import eplscout.db.DBUtil;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;

import org.springframework.stereotype.Repository;

@Repository
public class PlayerDao {

    /* =========================
       1. 고정 정보 UPSERT
    ========================= */
    public void upsertPlayerBase(
            int apiPlayerId,
            String name,
            LocalDate birthDate,
            String photoUrl
    ) {

        String sql = """
            INSERT INTO player (api_player_id, name, birth_date, photo_url)
            VALUES (?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                name = VALUES(name),
                birth_date = VALUES(birth_date),
                photo_url = VALUES(photo_url)
        """;

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, apiPlayerId);
            ps.setString(2, name);

            if (birthDate != null)
                ps.setDate(3, java.sql.Date.valueOf(birthDate));
            else
                ps.setNull(3, Types.DATE);

            ps.setString(4, photoUrl);
            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException("player base UPSERT 실패", e);
        }
    }

    /* =========================
       2. 시즌성 정보 UPDATE
    ========================= */
    public void updateSeasonInfo(
            int apiPlayerId,
            Integer age,
            String position
    ) {

        String sql = """
            UPDATE player
            SET age = ?, position = ?
            WHERE api_player_id = ?
        """;

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (age != null) ps.setInt(1, age);
            else ps.setNull(1, Types.INTEGER);

            if (position != null) ps.setString(2, position);
            else ps.setNull(2, Types.VARCHAR);

            ps.setInt(3, apiPlayerId);
            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException("player season info UPDATE 실패", e);
        }
    }

    /* =====================================================
       2-1. 확장 정보 UPDATE (선수번호 / 국적 / 키 / 몸무게)
    ===================================================== */
    public void updateExtraInfo(
            int apiPlayerId,
            Integer shirtNumber,
            String nationality,
            String height,
            String weight
    ) {

        String sql = """
            UPDATE player
            SET shirt_number = ?,
                nationality = ?,
                height = ?,
                weight = ?
            WHERE api_player_id = ?
        """;

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (shirtNumber != null) ps.setInt(1, shirtNumber);
            else ps.setNull(1, Types.INTEGER);

            ps.setString(2, nationality);
            ps.setString(3, height);
            ps.setString(4, weight);
            ps.setInt(5, apiPlayerId);

            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException("player 확장 정보 UPDATE 실패", e);
        }
    }

    /* =====================================================
        선수 전체 통합 UPSERT (PlayerApiService용)
    ===================================================== */
    public void upsertFullPlayer(
            int apiPlayerId,
            String name,
            LocalDate birthDate,
            String photoUrl,
            Integer age,
            String position,
            Integer shirtNumber,
            String nationality,
            String height,
            String weight
    ) {
        upsertPlayerBase(apiPlayerId, name, birthDate, photoUrl);
        updateSeasonInfo(apiPlayerId, age, position);
        updateExtraInfo(apiPlayerId, shirtNumber, nationality, height, weight);
    }

    /* =========================
       3. 내부 PK 조회
    ========================= */
    public long findPlayerIdByApiPlayerId(int apiPlayerId) {

        String sql = "SELECT player_id FROM player WHERE api_player_id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, apiPlayerId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("player_id");
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("player_id 조회 실패", e);
        }

        return 0L;
    }

    /* =========================
       4. 포지션별 선수 수
    ========================= */
    public Map<String, Integer> countPlayersByPosition(int teamId, int season) {

        String sql = """
            SELECT p.position, COUNT(*) AS cnt
            FROM player p
            JOIN player_season_stat ps
              ON p.player_id = ps.player_id
            WHERE ps.team_id = ?
              AND ps.season = ?
            GROUP BY p.position
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
                            rs.getInt("cnt")
                    );
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("포지션별 선수 수 조회 실패", e);
        }

        return result;
    }

    /* =========================
       5. 평균 연령 / 평균 평점
    ========================= */
    public Map<String, Object> findTeamAverageStat(int teamId, int season) {

        String sql = """
            SELECT
                AVG(p.age) AS avg_age,
                AVG(ps.avg_rating) AS avg_rating
            FROM player p
            JOIN player_season_stat ps
              ON p.player_id = ps.player_id
            WHERE ps.team_id = ?
              AND ps.season = ?
        """;

        Map<String, Object> map = new HashMap<>();

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, teamId);
            ps.setInt(2, season);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    map.put("avgAge", rs.getDouble("avg_age"));
                    map.put("avgRating", rs.getDouble("avg_rating"));
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("팀 평균 스탯 조회 실패", e);
        }

        return map;
    }

    /* =========================
       6. 약점 포지션
    ========================= */
    public String findWeakPosition(int teamId, int season) {

        String sql = """
            SELECT p.position
            FROM player p
            JOIN player_season_stat ps
              ON p.player_id = ps.player_id
            WHERE ps.team_id = ?
              AND ps.season = ?
            GROUP BY p.position
            ORDER BY COUNT(*) ASC
            LIMIT 1
        """;

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, teamId);
            ps.setInt(2, season);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("position");
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("약점 포지션 조회 실패", e);
        }

        return "N/A";
    }

    /* =====================================================
       팀 요약 통합 메서드
    ===================================================== */
    public Map<String, Object> getTeamSummary(int teamId, int season) {

        Map<String, Object> result = new HashMap<>();

        Map<String, Object> avg =
                findTeamAverageStat(teamId, season);

        result.putAll(avg);

        Map<String, Integer> positionCounts =
                countPlayersByPosition(teamId, season);

        result.put("positionCounts", positionCounts);

        String weak =
                findWeakPosition(teamId, season);

        result.put("weakestPositionByRating", weak);

        return result;
    }

    /* =====================================================
   팀 선수 목록 
===================================================== */
public List<Map<String, Object>> getPlayersByTeamAndSeason(int teamId, int season) {

    String sql = """
        SELECT
            p.player_id,
            p.name,
            p.position,
            p.age,
            p.shirt_number,
            p.nationality,
            p.photo_url,
            pis.injury_games
        FROM player p
        JOIN player_season_stat ps
          ON p.player_id = ps.player_id
        LEFT JOIN player_injury_stat pis
          ON p.player_id = pis.player_id
         AND pis.season = ?
        WHERE ps.team_id = ?
          AND ps.season = ?
        ORDER BY p.shirt_number ASC
    """;

    List<Map<String, Object>> list = new ArrayList<>();

    try (Connection conn = DBUtil.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {

        ps.setInt(1, season);
        ps.setInt(2, teamId);
        ps.setInt(3, season);

        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {

                Map<String, Object> row = new HashMap<>();
                row.put("playerId", rs.getLong("player_id"));
                row.put("name", rs.getString("name"));
                row.put("position", rs.getString("position"));
                row.put("age", rs.getInt("age"));
                row.put("shirtNumber", rs.getInt("shirt_number"));
                row.put("nationality", rs.getString("nationality"));
                row.put("photoUrl", rs.getString("photo_url"));

                //  빠른 부상 판단 로직
                int injuryGames = rs.getInt("injury_games");
                row.put("isInjured", injuryGames > 0);

                list.add(row);
            }
        }

    } catch (Exception e) {
        throw new RuntimeException("팀 선수 목록 조회 실패", e);
    }

    return list;
}



    /* =====================================================
       선수 시즌 상세 조회 
    ===================================================== */
    public Map<String, Object> getPlayerSeasonDetail(long playerId, int season) {

        String sql = """
            SELECT
                p.name,
                p.position,
                ps.appearances,
                ps.avg_rating,
                ps.goals,
                ps.assists,
                ps.shots,
                ps.key_passes,
                ps.pass_accuracy,
                ps.tackles,
                ps.interceptions,
                ps.clearances,
                ps.saves,
                ps.goals_conceded
            FROM player p
            JOIN player_season_stat ps
              ON p.player_id = ps.player_id
            WHERE p.player_id = ?
              AND ps.season = ?
        """;

        Map<String, Object> map = new HashMap<>();

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, playerId);
            ps.setInt(2, season);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    map.put("name", rs.getString("name"));
                    map.put("position", rs.getString("position"));
                    map.put("appearances", rs.getInt("appearances"));
                    map.put("avgRating", rs.getDouble("avg_rating"));
                    map.put("goals", rs.getInt("goals"));
                    map.put("assists", rs.getInt("assists"));
                    map.put("shots", rs.getInt("shots"));
                    map.put("keyPasses", rs.getInt("key_passes"));
                    map.put("passAccuracy", rs.getDouble("pass_accuracy"));
                    map.put("tackles", rs.getInt("tackles"));
                    map.put("interceptions", rs.getInt("interceptions"));
                    map.put("clearances", rs.getInt("clearances"));
                    map.put("saves", rs.getInt("saves"));
                    map.put("goalsConceded", rs.getInt("goals_conceded"));
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("선수 시즌 상세 조회 실패", e);
        }

        return map;
    }
}
