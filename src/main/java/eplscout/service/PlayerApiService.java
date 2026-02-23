package eplscout.service;

import eplscout.dao.PlayerDao;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;

import org.springframework.stereotype.Service;

@Service
public class PlayerApiService {

    private static final String API_KEY = "a170bd56a00dfe33e79d77ee714398ac";
    private static final String API_URL =
            "https://v3.football.api-sports.io/players";

    private final PlayerDao playerDao = new PlayerDao();

    public void loadTeamPlayers(int apiTeamId, int season) throws Exception {

        HttpClient client = HttpClient.newHttpClient();

        int page = 1;
        int totalPages = 1;

        do {

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL +
                            "?league=39" +
                            "&season=" + season +
                            "&team=" + apiTeamId +
                            "&page=" + page))
                    .header("x-apisports-key", API_KEY)
                    .GET()
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            JSONObject json = new JSONObject(response.body());

            // paging 정보 읽기
            if (json.has("paging")) {
                totalPages = json.getJSONObject("paging").optInt("total", 1);
            }

            JSONArray players = json.getJSONArray("response");

            for (int i = 0; i < players.length(); i++) {

                JSONObject p = players.getJSONObject(i);
                JSONObject player = p.getJSONObject("player");
                JSONObject stat =
                        p.getJSONArray("statistics").getJSONObject(0);

                JSONObject games = stat.getJSONObject("games");

                int apiPlayerId = player.getInt("id");
                String name = player.getString("name");
                String photo = player.optString("photo", null);

                Integer age = player.optInt("age", 0);
                if (age == 0) age = null;

                String nationality = player.optString("nationality", null);

                String height = player.optString("height", null);
                String weight = player.optString("weight", null);

                Integer shirtNumber = games.optInt("number", 0);
                if (shirtNumber == 0) shirtNumber = null;

                String position = games.optString("position", null);

                // birth_date
                LocalDate birthDate = null;
                if (!player.isNull("birth")) {
                    String date = player.getJSONObject("birth")
                                        .optString("date", null);
                    if (date != null) birthDate = LocalDate.parse(date);
                }

                // DB 저장
                playerDao.upsertFullPlayer(
                        apiPlayerId,
                        name,
                        birthDate,
                        photo,
                        age,
                        position,
                        shirtNumber,
                        nationality,
                        height,
                        weight
                );
            }

            page++;

        } while (page <= totalPages);
    }
}
