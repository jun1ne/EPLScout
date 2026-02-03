package dao;

import db.DBUtil;
import model.Team;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;


/**
 * TeamDao
 * - team 테이블에 접근(SQL 실행)하는 전담 클래스
 * - Service(API)와 DB(SQL)를 분리해서 유지보수하기 쉬움
 *
 * DAO 패턴으로 데이터 접근 계층 분리
 */
public class TeamDao {

    /**
     * 팀 리스트를 DB에 저장 (중복 방지 + 갱신 가능)
     *
     * - team 테이블에는 UNIQUE(api_team_id, season)이 걸려 있음
     * - 같은 팀/같은 시즌이 이미 있으면 중복 INSERT가 아니라 UPDATE가 되게 해야 함
     * - 이때 쓰는 게 UPSERT (ON DUPLICATE KEY UPDATE)
     */
    public int upsertTeams(List<Team> teams) throws Exception {

        String sql = """
            INSERT INTO team
            (api_team_id, name, country, founded_year, logo_url,
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

                // INSERT에 들어갈 값 세팅 (물음표 순서대로)
                ps.setInt(1, t.getApiTeamId());
                ps.setString(2, t.getName());
                ps.setString(3, t.getCountry());
                ps.setInt(4, t.getFoundedYear());
                ps.setString(5, t.getLogoUrl());

                ps.setString(6, t.getStadiumName());
                ps.setString(7, t.getStadiumCity());
                ps.setInt(8, t.getStadiumCapacity());

                // NOT NULL 컬럼이라 무조건 있어야 저장 가능
                ps.setInt(9, t.getLeagueId());
                ps.setInt(10, t.getSeason());

                // 실행
                affected += ps.executeUpdate();
            }
        }

        return affected;
    }

    // 내부 team_id조회
    public long findTeamIdByApiTeamIdAndSeason(int apiTeamId, int season) throws Exception {

    String sql = "SELECT team_id FROM team WHERE api_team_id = ? AND season = ?";

    try (Connection conn = DBUtil.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {

        ps.setInt(1, apiTeamId);
        ps.setInt(2, season);

        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getLong("team_id");
            }
        }
    }

    return 0L;
}

/**
 * team 테이블에서 특정 시즌(EPL 2023)의 api_team_id 목록 조회
 * - Step B에서 20팀을 돌리기 위해 필요
 */
public List<Integer> findApiTeamIdsBySeason(int season) throws Exception {

    String sql = "SELECT api_team_id FROM team WHERE season = ? ORDER BY api_team_id";

    List<Integer> ids = new ArrayList<>();

    try (Connection conn = DBUtil.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {

        ps.setInt(1, season);

        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ids.add(rs.getInt("api_team_id"));
            }
        }
    }

    return ids;
}


}
