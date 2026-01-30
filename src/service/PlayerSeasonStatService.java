package service;

import dao.PlayerDao;
import dao.PlayerSeasonStatDao;
import dao.TeamDao;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * PlayerSeasonStatService
 *
 * 책임
 * - 팀/시즌 단위 시즌 누적 스탯 적재 총괄
 * - API → 내부 PK 매핑
 * - DAO 호출
 *
 * 실질적인 메인 로직
 */
public class PlayerSeasonStatService {

    private final PlayerSeasonStatApiService apiService =
            new PlayerSeasonStatApiService();

    private final PlayerSeasonStatDao statDao =
            new PlayerSeasonStatDao();

    private final PlayerDao playerDao =
            new PlayerDao();

    private final TeamDao teamDao =
            new TeamDao();

    /**
     * 팀 1개 시즌 스탯 적재
     */
    public void loadTeamSeasonStats(
            int leagueId,
            int season,
            int apiTeamId
    ) throws Exception {

        long teamId =
                teamDao.findTeamIdByApiTeamIdAndSeason(apiTeamId, season);

        if (teamId == 0L) {
            System.out.println("[SKIP] team_id 없음 apiTeamId=" + apiTeamId);
            return;
        }

        int page = 1;
        JSONObject first =
                apiService.fetchPlayersStatPage(
                        leagueId, season, apiTeamId, page);

        int totalPages =
                apiService.getTotalPages(first);

        while (page <= totalPages) {

            JSONObject json =
                    apiService.fetchPlayersStatPage(
                            leagueId, season, apiTeamId, page);

            JSONArray players =
                    apiService.getResponseArray(json);

            System.out.println(
                "[DEBUG] apiTeamId=" + apiTeamId +
                " page=" + page +
                " players=" + players.length()
            );

            for (int i = 0; i < players.length(); i++) {

                JSONObject obj = players.getJSONObject(i);

                JSONObject playerJson = obj.getJSONObject("player");
                int apiPlayerId = playerJson.getInt("id");

                long playerId =
                        playerDao.findPlayerIdByApiPlayerId(apiPlayerId);

                if (playerId == 0L) continue;

                JSONArray stats = obj.getJSONArray("statistics");
                if (stats.length() == 0) continue;

                // 수정 부분
                JSONObject stat = null;

                for (int j = 0; j < stats.length(); j++) {
                    JSONObject s = stats.getJSONObject(j);
                    JSONObject league = s.getJSONObject("league");

                    if (league.getInt("id") == leagueId
                            && league.getInt("season") == season) {
                        stat = s;
                        break;
                    }
                }

                if (stat == null) continue;
                // 여기까지

                JSONObject games = stat.getJSONObject("games");

                int appearances =
                        games.optInt("appearences", 0); // API 스펙 그대로
                int minutes =
                        games.optInt("minutes", 0);

                Double rating = null;
                String ratingStr =
                        games.optString("rating", null);
                if (ratingStr != null && !ratingStr.isEmpty()) {
                    rating = Double.parseDouble(ratingStr);
                }

                /**
                 * DAO 적재
                 */
                statDao.upsert(
                        playerId,
                        teamId,
                        season,
                        appearances,
                        minutes,
                        rating
                );
            }

            page++;
        }
    }
}
