package eplscout.dao;

import eplscout.db.DBUtil;
import eplscout.model.RosterEntry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

import org.springframework.stereotype.Repository;

/**
 * RosterDao
 * - team_roster 테이블 저장(UPSERT) 전담
 *
 * - UNIQUE(team_id, season, player_id)로 중복 방지
 */
@Repository
public class RosterDao {

    public int upsertRoster(List<RosterEntry> rosterEntries) throws Exception {

        String sql = """
            INSERT INTO team_roster
            (team_id, season, player_id, squad_number, position_snapshot)
            VALUES (?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                squad_number = VALUES(squad_number),
                position_snapshot = VALUES(position_snapshot)
        """;

        int affected = 0;

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (RosterEntry r : rosterEntries) {
                ps.setLong(1, r.getTeamId());
                ps.setInt(2, r.getSeason());
                ps.setLong(3, r.getPlayerId());
                ps.setInt(4, r.getSquadNumber());
                ps.setString(5, r.getPositionSnapshot());

                affected += ps.executeUpdate();
            }
        }

        return affected;
    }
}
