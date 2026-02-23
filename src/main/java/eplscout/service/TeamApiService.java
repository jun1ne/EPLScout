package eplscout.service;

import eplscout.model.Team;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * TeamApiService
 *
 * 책임:
 * - 외부 API 통신 전담
 * - 시즌 파라미터 기반 팀 목록 조회
 *
 * 시즌 고정 제거 → 배치 루프 가능하도록 개선
 */
@Service
public class TeamApiService {

    private static final String API_KEY =
            "a170bd56a00dfe33e79d77ee714398ac";

    private static final String BASE_URL =
            "https://v3.football.api-sports.io";

    /**
     * 리그 + 시즌 기반 팀 조회
     */
    public List<Team> fetchTeams(int leagueId, int season)
            throws Exception {

        List<Team> teamList = new ArrayList<>();

        String url =
                BASE_URL + "/teams"
                + "?league=" + leagueId
                + "&season=" + season;

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("x-apisports-key", API_KEY)
                        .GET()
                        .build();

        HttpResponse<String> response =
                HttpClient.newHttpClient()
                        .send(request,
                              HttpResponse.BodyHandlers.ofString());

        JSONObject json =
                new JSONObject(response.body());

        if (!json.has("response")) {
            return teamList;
        }

        JSONArray teams =
                json.getJSONArray("response");

        for (int i = 0; i < teams.length(); i++) {

            JSONObject item =
                    teams.getJSONObject(i);

            JSONObject teamJson =
                    item.getJSONObject("team");

            JSONObject venueJson =
                    item.optJSONObject("venue");

            Team team = new Team(
                    teamJson.getInt("id"),
                    teamJson.getString("name"),
                    teamJson.getString("country"),
                    teamJson.optInt("founded", 0),
                    teamJson.optString("logo", null),
                    venueJson != null
                        ? venueJson.optString("name", null)
                        : null,
                    venueJson != null
                        ? venueJson.optString("city", null)
                        : null,
                    venueJson != null
                        ? venueJson.optInt("capacity", 0)
                        : 0,
                    leagueId,
                    season
            );

            teamList.add(team);
        }

        return teamList;
    }

    /**
     * 기존 코드 호환용 메서드
     * (TeamService 등에서 getEplTeams()를 호출하고 있기 때문에 유지)
     *
     * 기존과 동일하게 EPL(39), 2023 시즌 반환
     */
    public List<Team> getEplTeams() throws Exception {
        return fetchTeams(39, 2023);
    }
}
