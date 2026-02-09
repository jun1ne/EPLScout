package eplscout.dao;

import eplscout.db.DBUtil;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

/**
 * LeagueStandingDao
 *
 * 책임
 * - 리그 순위(standings) DB 저장
 * - 메인보드 화면용 리그 순위 조회
 *
 * 설계 원칙
 * - 계산 로직 없음
 * - API / Service에서 가공된 값만 저장
 * - 조회 전용 데이터 성격
 */
@Repository
public class LeagueStandingDao {

    /* ==================================================
       리그 순위 UPSERT (배치 적재용)
       ================================================== */
    public void upsert(
            int leagueId,
            int season,
            long teamId,
            int rank,
            int played,
            int win,
            int draw,
            int lose,
            int points,
            int goalsFor,
            int goalsAgainst,
            int goalDiff
    ) {

        String sql = """
            INSERT INTO league_standing
                (league_id, season, team_id,
                 `rank`, played, win, draw, lose,
                 points, goals_for, goals_against, goal_diff)
            VALUES
                (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                `rank` = VALUES(`rank`),
                played = VALUES(played),
                win = VALUES(win),
                draw = VALUES(draw),
                lose = VALUES(lose),
                points = VALUES(points),
                goals_for = VALUES(goals_for),
                goals_against = VALUES(goals_against),
                goal_diff = VALUES(goal_diff)
        """;

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, leagueId);
            ps.setInt(2, season);
            ps.setLong(3, teamId);
            ps.setInt(4, rank);
            ps.setInt(5, played);
            ps.setInt(6, win);
            ps.setInt(7, draw);
            ps.setInt(8, lose);
            ps.setInt(9, points);
            ps.setInt(10, goalsFor);
            ps.setInt(11, goalsAgainst);
            ps.setInt(12, goalDiff);

            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException("리그 순위 저장 실패", e);
        }
    }

    /* ==================================================
       메인보드 조회용 리그 순위 리스트
       ================================================== */
    public List<Map<String, Object>> findByLeagueAndSeason(
            int leagueId,
            int season
    ) {

        String sql = """
            SELECT
                ls.rank,
                t.name AS team_name,
                ls.played,
                ls.win,
                ls.draw,
                ls.lose,
                ls.points,
                ls.goals_for,
                ls.goals_against,
                ls.goal_diff
            FROM league_standing ls
            JOIN team t ON ls.team_id = t.team_id
            WHERE ls.league_id = ?
              AND ls.season = ?
            ORDER BY ls.rank ASC
        """;

        List<Map<String, Object>> list = new ArrayList<>();

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, leagueId);
            ps.setInt(2, season);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {

                    Map<String, Object> row = new HashMap<>();

                    row.put("rank", rs.getInt("rank"));
                    row.put("teamName", rs.getString("team_name"));
                    row.put("played", rs.getInt("played"));
                    row.put("win", rs.getInt("win"));
                    row.put("draw", rs.getInt("draw"));
                    row.put("lose", rs.getInt("lose"));
                    row.put("points", rs.getInt("points"));
                    row.put("goalsFor", rs.getInt("goals_for"));
                    row.put("goalsAgainst", rs.getInt("goals_against"));
                    row.put("goalDiff", rs.getInt("goal_diff"));

                    list.add(row);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("리그 순위 조회 실패", e);
        }

        return list;
    }
}
