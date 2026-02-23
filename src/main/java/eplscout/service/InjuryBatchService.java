package eplscout.service;

import eplscout.db.DBUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * InjuryBatchService
 *
 * 역할:
 * 1. API-Football Injuries API 호출
 * 2. player_injury_history 테이블에 저장
 * 3. 시즌 단위 반복 실행 가능
 */
@Service
public class InjuryBatchService {

    /* 기존 RapidAPI 제거 → api-sports.io로 통일 */
    private static final String API_KEY =
            "a170bd56a00dfe33e79d77ee714398ac";

    private static final String BASE_URL =
            "https://v3.football.api-sports.io";

    public void collectSeasonInjuries(int leagueId, int season) {

        try {

            /*  도메인 통일 */
            String urlStr =
                    BASE_URL + "/injuries"
                            + "?league=" + leagueId
                            + "&season=" + season;

            URL url = new URL(urlStr);
            HttpURLConnection conn =
                    (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");

            /*  헤더 통일 */
            conn.setRequestProperty("x-apisports-key", API_KEY);

            int responseCode = conn.getResponseCode();

            /*  403 등 실패해도 배치 안죽게 방어 */
            if (responseCode != 200) {
                System.out.println("부상 API 호출 실패: HTTP " + responseCode);
                return;
            }

            BufferedReader in =
                    new BufferedReader(
                            new InputStreamReader(
                                    conn.getInputStream()));

            StringBuilder response = new StringBuilder();
            String line;

            while ((line = in.readLine()) != null) {
                response.append(line);
            }

            in.close();

            JSONObject json =
                    new JSONObject(response.toString());

            JSONArray injuries =
                    json.getJSONArray("response");

            saveToDatabase(injuries, season);

        } catch (Exception e) {

            /*  부상 API 실패해도 전체 시즌 배치 안 멈춤 */
            System.out.println("부상 API 수집 중 오류 발생 (계속 진행): "
                    + e.getMessage());
        }
    }

    /**
     * player_injury_history 저장
     */
    private void saveToDatabase(
            JSONArray injuries,
            int season) throws Exception {

        String sql = """
            INSERT INTO player_injury_history
            (player_id, season, injury_type,
             status, fixture_date)
            VALUES (?, ?, ?, ?, ?)
        """;

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps =
                     conn.prepareStatement(sql)) {

            for (int i = 0; i < injuries.length(); i++) {

                JSONObject obj = injuries.getJSONObject(i);

                long playerId =
                        obj.getJSONObject("player")
                           .getLong("id");

                String injuryType =
                        obj.optString("reason", null);

                String status =
                        obj.optString("type", null);

                String date =
                        obj.getJSONObject("fixture")
                           .optString("date", "")
                           .substring(0, 10);

                ps.setLong(1, playerId);
                ps.setInt(2, season);
                ps.setString(3, injuryType);
                ps.setString(4, status);
                ps.setString(5, date);

                ps.addBatch();
            }

            ps.executeBatch();
        }
    }
}
