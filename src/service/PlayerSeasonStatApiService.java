package service;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * PlayerSeasonStatApiService
 *
 * 책임
 * - /players 엔드포인트 호출 전담
 * - 팀/시즌 기준 선수 시즌 누적 스탯 조회
 * - page 단위 JSON 원문 반환
 *
 * DB, DAO 모름
 */
public class PlayerSeasonStatApiService {

    private static final String API_KEY = "a170bd56a00dfe33e79d77ee714398ac";
    private static final String BASE_URL = "https://v3.football.api-sports.io";

    private final HttpClient client = HttpClient.newHttpClient();

    public JSONObject fetchPlayersStatPage(
            int leagueId,
            int season,
            int apiTeamId,
            int page
    ) throws Exception {

        String url = BASE_URL + "/players"
                + "?league=" + leagueId
                + "&season=" + season
                + "&team=" + apiTeamId
                + "&page=" + page;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("x-apisports-key", API_KEY)
                .GET()
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        return new JSONObject(response.body());
    }

    public int getTotalPages(JSONObject json) {
        if (!json.has("paging")) return 1;
        return json.getJSONObject("paging").optInt("total", 1);
    }

    public JSONArray getResponseArray(JSONObject json) {
        if (!json.has("response")) return new JSONArray();
        return json.getJSONArray("response");
    }
}
