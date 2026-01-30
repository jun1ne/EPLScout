package service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;

import org.json.JSONArray;
import org.json.JSONObject;

import dao.PlayerDao;
import dao.PlayerMatchStatDao;
import dao.TeamDao;

/**
 * PlayerMatchStatApiService
 *
 * - 선수 "경기 단위" 스탯 적재 전담 클래스
 * - 추천/분석에 쓰이는 원천 데이터의 시작점
 *
 * 핵심 설계
 * - 외부 API ID → 내부 PK 변환 후 저장
 * - 출전 기록이 없어도 (0분, NULL 평점) 원천 데이터로 보존
 */
public class PlayerMatchStatApiService {

    private static final String API_KEY = "a170bd56a00dfe33e79d77ee714398ac";

    private static final String API_URL =
            "https://v3.football.api-sports.io/fixtures/players";

    private final PlayerMatchStatDao playerMatchStatDao = new PlayerMatchStatDao();
    private final PlayerDao playerDao = new PlayerDao();
    private final TeamDao teamDao = new TeamDao();

    public void loadTeamPlayerMatchStats(int apiTeamId, int season)
            throws Exception {

        // 외부 team.id → 내부 team_id
        long teamId =
                teamDao.findTeamIdByApiTeamIdAndSeason(apiTeamId, season);

        if (teamId == 0L) {
            System.out.println(
                "[ERROR] team_id 매핑 실패 apiTeamId=" + apiTeamId + ", season=" + season
            );
            return;
        }

        int page = 1;
        int totalPages = 1;

        while (page <= totalPages) {

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL
                            + "?league=39"
                            + "&season=" + season
                            + "&team=" + apiTeamId
                            + "&page=" + page))
                    .header("x-apisports-key", API_KEY)
                    .GET()
                    .build();

            HttpResponse<String> response =
                    HttpClient.newHttpClient()
                            .send(request, HttpResponse.BodyHandlers.ofString());

            JSONObject json = new JSONObject(response.body());

            if (!json.has("response")) {
                System.out.println("[WARN] fixtures/players 응답 없음");
                return;
            }

            JSONObject paging = json.getJSONObject("paging");
            totalPages = paging.getInt("total");

            JSONArray fixtures = json.getJSONArray("response");
            System.out.println(
                "[DEBUG] apiTeamId=" + apiTeamId +
                " fixturesCount=" + fixtures.length()
                );

            for (int i = 0; i < fixtures.length(); i++) {

                JSONObject fixture = fixtures.getJSONObject(i);

                long matchId =
                        fixture.getJSONObject("fixture").getLong("id");

                String dateStr =
                        fixture.getJSONObject("fixture").getString("date");
                LocalDate matchDate =
                        LocalDate.parse(dateStr.substring(0, 10));

                JSONArray players =
                        fixture.getJSONArray("players");
                        System.out.println(
                        "[DEBUG] matchId=" + matchId +
                        " playersCount=" + players.length()
                        );

                for (int j = 0; j < players.length(); j++) {

                    JSONObject playerObj = players.getJSONObject(j);

                    int apiPlayerId =
                            playerObj.getJSONObject("player").getInt("id");

                    long playerId =
                            playerDao.findPlayerIdByApiPlayerId(apiPlayerId);

                    // player 없으면 먼저 생성
                    if (playerId == 0L) {
                        String name =
                                playerObj.getJSONObject("player").getString("name");
                        String photo =
                                playerObj.getJSONObject("player").optString("photo", null);

                        playerDao.upsertSinglePlayer(apiPlayerId, name, photo);
                        playerId =
                                playerDao.findPlayerIdByApiPlayerId(apiPlayerId);
                    }

                    if (playerId == 0L) continue;
                    

                    // ===== 여기부터가 핵심 수정 =====
                    JSONArray stats =
                            playerObj.getJSONArray("statistics");
                            System.out.println(
                                "[DEBUG] apiPlayerId=" + apiPlayerId +
                                " statsCount=" + stats.length()
                                );


                    int minutes = 0;
                    Double rating = null;

                    if (stats.length() > 0) {
                        JSONObject stat = stats.getJSONObject(0);
                        JSONObject games = stat.getJSONObject("games");

                        minutes = games.optInt("minutes", 0);

                        String ratingStr = games.optString("rating", null);
                        if (ratingStr != null && !ratingStr.isEmpty()) {
                            rating = Double.parseDouble(ratingStr);
                        }
                    }

                    // stats가 없어도 반드시 INSERT
                    playerMatchStatDao.upsert(
                            playerId,
                            teamId,
                            matchId,
                            season,
                            matchDate,
                            minutes,
                            rating
                    );
                }
            }

            page++;
        }
    }
}
