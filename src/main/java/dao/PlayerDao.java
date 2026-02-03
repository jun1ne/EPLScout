package dao;

import db.DBUtil;

import java.sql.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PlayerDao
 *
 * 책임
 * 1. 선수 고정 정보 저장 (birth_date 포함)
 * 2. 시즌성 정보(age, position) 갱신
 * 3. api_player_id → 내부 player_id 매핑
 */
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

            if (birthDate != null) {
                ps.setDate(3, Date.valueOf(birthDate));
            } else {
                ps.setNull(3, Types.DATE);
            }

            ps.setString(4, photoUrl);
            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException(
                "player base UPSERT 실패 (apiPlayerId=" + apiPlayerId + ")", e
            );
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
            throw new RuntimeException(
                "player season info UPDATE 실패 (apiPlayerId=" + apiPlayerId + ")", e
            );
        }
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
       4. roster용 ID 매핑
       ========================= */
    public Map<Integer, Long> findPlayerIdMapByApiPlayerIds(List<Integer> apiPlayerIds) {

        Map<Integer, Long> map = new HashMap<>();
        if (apiPlayerIds == null || apiPlayerIds.isEmpty()) return map;

        StringBuilder sb = new StringBuilder(
            "SELECT api_player_id, player_id FROM player WHERE api_player_id IN ("
        );
        for (int i = 0; i < apiPlayerIds.size(); i++) {
            sb.append("?");
            if (i < apiPlayerIds.size() - 1) sb.append(",");
        }
        sb.append(")");

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sb.toString())) {

            for (int i = 0; i < apiPlayerIds.size(); i++) {
                ps.setInt(i + 1, apiPlayerIds.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    map.put(
                        rs.getInt("api_player_id"),
                        rs.getLong("player_id")
                    );
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("player_id map 조회 실패", e);
        }

        return map;
    }
}
