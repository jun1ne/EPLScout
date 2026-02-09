package eplscout.service;

import eplscout.model.Player;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * SquadApiService
 * - players/squads 엔드포인트 호출 전담
 * - 오직: 특정 팀의 스쿼드(선수 목록)를 가져와 Player 리스트로 변환
 *
 * - DB 저장/추천/GUI는 모름 -> API 통신 책임만 가짐
 */
public class SquadApiService {

    private static final String API_KEY = "a170bd56a00dfe33e79d77ee714398ac";
    private static final String BASE_URL = "https://v3.football.api-sports.io";

    /**
     * 팀 스쿼드 가져오기
     * @param apiTeamId API 팀 ID 
     * @return squads 응답의 players를 Player 리스트로 변환한 결과
     */
    public List<Player> getSquadPlayers(int apiTeamId) throws Exception {

        List<Player> players = new ArrayList<>();

        String url = BASE_URL + "/players/squads?team=" + apiTeamId;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("x-apisports-key", API_KEY)
                .GET()
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        JSONObject json = new JSONObject(response.body());

        // 방어 코드: 실패 응답이면 response가 없을 수 있음
        if (!json.has("response")) {
            System.out.println("API Error (response 키 없음):");
            System.out.println(json.toString(2));
            return players;
        }

        JSONArray resp = json.getJSONArray("response");
        if (resp.isEmpty()) {
            // 팀 ID가 잘못됐거나 데이터가 없는 경우
            return players;
        }

        // response[0].players[] 안에 선수 목록이 들어있음
        JSONObject first = resp.getJSONObject(0);
        JSONArray playerArr = first.getJSONArray("players");

        for (int i = 0; i < playerArr.length(); i++) {
            JSONObject p = playerArr.getJSONObject(i);

            int apiPlayerId = p.getInt("id");
            String name = p.getString("name");
            int age = p.optInt("age", 0);

            // number는 없는 선수도 있을 수 있으니 optInt로 안전 처리
            int number = p.optInt("number", 0);

            String position = p.optString("position", null);
            String photo = p.optString("photo", null);

            Player player = new Player(apiPlayerId, name, age, number, position, photo);
            players.add(player);

        }

        return players;
    }
}
