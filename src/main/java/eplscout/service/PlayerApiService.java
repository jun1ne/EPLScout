package eplscout.service;

import eplscout.dao.PlayerDao;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;

/**
 * PlayerApiService
 *
 * - players API 전담
 * - 선수 기본 정보 + 시즌 정보 분리 적재
 */
public class PlayerApiService {

    private static final String API_KEY = "a170bd56a00dfe33e79d77ee714398ac";
    private static final String API_URL =
            "https://v3.football.api-sports.io/players";

    private final PlayerDao playerDao = new PlayerDao();

    public void loadTeamPlayers(int apiTeamId, int season) throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL +
                        "?league=39" +
                        "&season=" + season +
                        "&team=" + apiTeamId))
                .header("x-apisports-key", API_KEY)
                .GET()
                .build();

        HttpResponse<String> response =
                HttpClient.newHttpClient()
                        .send(request, HttpResponse.BodyHandlers.ofString());

        JSONObject json = new JSONObject(response.body());
        JSONArray players = json.getJSONArray("response");

        for (int i = 0; i < players.length(); i++) {

            JSONObject p = players.getJSONObject(i);
            JSONObject player = p.getJSONObject("player");
            JSONObject games =
                    p.getJSONArray("statistics")
                     .getJSONObject(0)
                     .getJSONObject("games");

            int apiPlayerId = player.getInt("id");
            String name = player.getString("name");
            String photo = player.optString("photo", null);

            // birth_date
            LocalDate birthDate = null;
            if (!player.isNull("birth")) {
                String date = player.getJSONObject("birth").optString("date", null);
                if (date != null) birthDate = LocalDate.parse(date);
            }

            // 1️⃣ 고정 정보
            playerDao.upsertPlayerBase(
                    apiPlayerId,
                    name,
                    birthDate,
                    photo
            );

            // 2️⃣ 시즌 정보
            Integer age = player.optInt("age", 0);
            if (age == 0) age = null;

            String position = games.optString("position", null);

            playerDao.updateSeasonInfo(
                    apiPlayerId,
                    age,
                    position
            );
        }
    }
}
