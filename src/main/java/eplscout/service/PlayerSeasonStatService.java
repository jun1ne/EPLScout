package eplscout.service;

import eplscout.dao.PlayerDao;
import eplscout.dao.PlayerSeasonStatDao;
import eplscout.dao.TeamDao;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * PlayerSeasonStatService
 *
 * 책임
 * - 팀/시즌 단위 시즌 누적 스탯 적재 총괄
 * - API → 내부 PK 매핑
 * - 확장 스탯까지 포함하여 DB 적재
 */
import org.springframework.stereotype.Service;

@Service
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
    ) 
    
    
    throws Exception {

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

            for (int i = 0; i < players.length(); i++) {

                JSONObject obj = players.getJSONObject(i);

                JSONObject playerJson = obj.getJSONObject("player");
                int apiPlayerId = playerJson.getInt("id");

                long playerId =
                        playerDao.findPlayerIdByApiPlayerId(apiPlayerId);

                if (playerId == 0L) continue;

                JSONArray stats = obj.getJSONArray("statistics");
                if (stats.length() == 0) continue;

                // =========================
                // 리그 + 시즌 필터링
                // =========================
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

                // =========================
                // 공통 베이스 스탯
                // =========================
                JSONObject games = stat.getJSONObject("games");

                int appearances =
                        games.optInt("appearences", 0);
                int minutes =
                        games.optInt("minutes", 0);

                Double rating = null;
                String ratingStr =
                        games.optString("rating", null);
                if (ratingStr != null && !ratingStr.isEmpty()) {
                    rating = Double.parseDouble(ratingStr);
                }

                // =========================
                // 공격 지표
                // =========================
                JSONObject goals = stat.optJSONObject("goals");
                int goalsTotal = goals != null ? goals.optInt("total", 0) : 0;
                int assists = goals != null ? goals.optInt("assists", 0) : 0;

                JSONObject shots = stat.optJSONObject("shots");
                int shotsTotal = shots != null ? shots.optInt("total", 0) : 0;

                // =========================
                // 패스 지표
                // =========================
                JSONObject passes = stat.optJSONObject("passes");
                int keyPasses = passes != null ? passes.optInt("key", 0) : 0;

                double passAccuracy = 0;
                if (passes != null) {
                    String accStr = passes.optString("accuracy", "0");
                    if (!accStr.isEmpty()) {
                        passAccuracy = Double.parseDouble(accStr);
                    }
                }

                // =========================
                // 수비 지표
                // =========================
                JSONObject tackles = stat.optJSONObject("tackles");
                int tacklesTotal =
                        tackles != null ? tackles.optInt("total", 0) : 0;
                int interceptions =
                        tackles != null ? tackles.optInt("interceptions", 0) : 0;

                JSONObject defense = stat.optJSONObject("defense");
                int clearances = defense != null ? defense.optInt("clearances", 0) : 0;



                // =========================
                // 골키퍼 지표
                // =========================
                JSONObject goalkeeper = stat.optJSONObject("goalkeeper");
                int saves =
                        goalkeeper != null ? goalkeeper.optInt("saves", 0) : 0;

                int goalsConceded =
                        goals != null ? goals.optInt("conceded", 0) : 0;

                // ==================================================
                // DAO 적재 (확장 스탯 포함)
                // ==================================================
                statDao.upsertWithExtendedStats(
                        playerId,
                        teamId,
                        season,

                        // base
                        appearances,
                        minutes,
                        rating,

                        // attack
                        goalsTotal,
                        assists,
                        shotsTotal,

                        // pass
                        keyPasses,
                        passAccuracy,

                        // defense
                        tacklesTotal,
                        interceptions,
                        clearances,

                        // goalkeeper
                        saves,
                        goalsConceded
                );
            }

            page++;
        }
    }
}
