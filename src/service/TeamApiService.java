package service;

import model.Team;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

// 외부 API 통신 전담 클래스
// DB, UI 모름 -> 오직 팀 목록을 가져오는 책임만 가짐
public class TeamApiService {

    private static final String API_KEY = "a170bd56a00dfe33e79d77ee714398ac";
    private static final String API_URL =
        "https://api-football-v1.p.rapidapi.com/v3/teams?league=39&season=2023";

    public List<Team> getEplTeams() throws Exception {

        List<Team> teamList = new ArrayList<>();

        // 1. 요청 생성
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("X-RapidAPI-Key", API_KEY)
                .header("X-RapidAPI-Host", "api-football-v1.p.rapidapi.com")
                .GET()
                .build();

        // 2. 요청 전송
        HttpResponse<String> response =
                HttpClient.newHttpClient()
                        .send(request, HttpResponse.BodyHandlers.ofString());

        // 3. JSON 파싱
        JSONObject json = new JSONObject(response.body());
        System.out.println(json.toString(2));
        JSONArray teams = json.getJSONArray("response");


        for (int i = 0; i < teams.length(); i++) {

            JSONObject teamJson =
                    teams.getJSONObject(i).getJSONObject("team");

            int apiTeamId = teamJson.getInt("id");
            String name = teamJson.getString("name");
            String country = teamJson.getString("country");
            int founded = teamJson.getInt("founded");

            Team team = new Team(apiTeamId, name, country, founded);
            teamList.add(team);
        }

        return teamList;
    }
}
