package eplscout.dao;

import eplscout.db.DBUtil;
import eplscout.model.Team;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

@Repository
public class TeamDao {

    /* =====================================================
       1. 팀 리스트 UPSERT (API → DB)
       ===================================================== */
    public int upsertTeams(List<Team> teams) throws Exception {

        String sql = """
            INSERT INTO team
            (team_id, name, country, founded_year, logo_url,
             stadium_name, stadium_city, stadium_capacity,
             league_id, season)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                name = VALUES(name),
                country = VALUES(country),
                founded_year = VALUES(founded_year),
                logo_url = VALUES(logo_url),
                stadium_name = VALUES(stadium_name),
                stadium_city = VALUES(stadium_city),
                stadium_capacity = VALUES(stadium_capacity),
                league_id = VALUES(league_id)
        """;

        int affected = 0;

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (Team t : teams) {
                ps.setInt(1, t.getApiTeamId());
                ps.setString(2, t.getName());
                ps.setString(3, t.getCountry());
                ps.setInt(4, t.getFoundedYear());
                ps.setString(5, t.getLogoUrl());
                ps.setString(6, t.getStadiumName());
                ps.setString(7, t.getStadiumCity());
                ps.setInt(8, t.getStadiumCapacity());
                ps.setInt(9, t.getLeagueId());
                ps.setInt(10, t.getSeason());

                affected += ps.executeUpdate();
            }
        }

        return affected;
    }

    /* =====================================================
       2. 내부 team_id 조회
       ===================================================== */
    public long findTeamIdByApiTeamIdAndSeason(int apiTeamId, int season) {

        String sql = """
            SELECT team_id
            FROM team
            WHERE team_id = ?
              AND season = ?
        """;

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, apiTeamId);
            ps.setInt(2, season);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("team_id");
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("팀 ID 조회 실패", e);
        }

        return 0L;
    }

    /* =====================================================
       3. 시즌 기준 API 팀 ID 목록
       ===================================================== */
    public List<Integer> findApiTeamIdsBySeason(int season) {

        String sql = """
            SELECT team_id
            FROM team
            WHERE season = ?
            ORDER BY team_id
        """;

        List<Integer> ids = new ArrayList<>();

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, season);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt("team_id"));
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("시즌별 팀 ID 조회 실패", e);
        }

        return ids;
    }

    /* =====================================================
       4. GUI 전용: 팀 목록 조회
       ===================================================== */
    public List<Map<String, Object>> findTeamsForView(int season) {

        String sql = """
            SELECT team_id, name
            FROM team
            WHERE season = ?
            ORDER BY name
        """;

        List<Map<String, Object>> list = new ArrayList<>();

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, season);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("teamId", rs.getLong("team_id"));
                    row.put("name", rs.getString("name"));
                    list.add(row);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("GUI 팀 목록 조회 실패", e);
        }

        return list;
    }

    /* =====================================================
       5. 팀 관리 화면용 단일 팀 조회 
       ===================================================== */
    public Map<String, Object> findTeamById(int teamId) {

        String sql = """
            SELECT team_id, name
            FROM team
            WHERE team_id = ?
        """;

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, teamId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("teamId", rs.getInt("team_id"));
                    map.put("name", rs.getString("name"));
                    return map;
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("팀 조회 실패", e);
        }

        return new HashMap<>();
    }

    /* =====================================================
       6. 배치용: 리그 + 시즌 기준 팀 조회
       ===================================================== */
    public List<Team> findTeamsByLeagueAndSeason(int leagueId, int season) {

        String sql = """
            SELECT team_id, name, league_id, season
            FROM team
            WHERE league_id = ?
              AND season = ?
            ORDER BY team_id
        """;

        List<Team> list = new ArrayList<>();

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, leagueId);
            ps.setInt(2, season);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {

                    Team team = new Team();
                    team.setTeamId(rs.getLong("team_id"));
                    team.setApiTeamId(rs.getInt("team_id"));
                    team.setName(rs.getString("name"));
                    team.setLeagueId(rs.getInt("league_id"));
                    team.setSeason(rs.getInt("season"));

                    list.add(team);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("리그/시즌 팀 조회 실패", e);
        }

        return list;
    }
}
