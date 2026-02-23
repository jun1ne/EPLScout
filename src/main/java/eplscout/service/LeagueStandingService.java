package eplscout.service;

import eplscout.dao.LeagueStandingDao;
import eplscout.dao.TeamDao;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

/**
 * LeagueStandingService
 *
 * 역할
 * - 외부 API-Football에서 리그 순위 조회
 * - 내부 team_id와 매핑
 * - DB 적재 (team_logo 포함)
 * - 메인 대시보드 조회용 데이터 제공
 */
@Service
public class LeagueStandingService {

    private final LeagueStandingDao leagueStandingDao;
    private final TeamDao teamDao;

    private final LeagueStandingApiService apiService =
            new LeagueStandingApiService();

    public LeagueStandingService(
            LeagueStandingDao leagueStandingDao,
            TeamDao teamDao
    ) {
        this.leagueStandingDao = leagueStandingDao;
        this.teamDao = teamDao;
    }

    /* ==================================================
       리그 순위 API → DB 적재
       ================================================== */
    public void fetchAndSaveStandings(
            int leagueId,
            int season
    ) throws Exception {

        JSONObject json = apiService.fetchStandings(leagueId, season);

        JSONArray standings =
                json.getJSONArray("response")
                        .getJSONObject(0)
                        .getJSONObject("league")
                        .getJSONArray("standings")
                        .getJSONArray(0);

        for (int i = 0; i < standings.length(); i++) {

            JSONObject row = standings.getJSONObject(i);

            int rank = row.getInt("rank");

            JSONObject team = row.getJSONObject("team");
            int apiTeamId = team.getInt("id");
            String teamLogo = team.getString("logo"); //  로고 추출

            long teamId =
                    teamDao.findTeamIdByApiTeamIdAndSeason(apiTeamId, season);

            if (teamId == 0L) continue;

            JSONObject all = row.getJSONObject("all");

            int played = all.getInt("played");
            int win = all.getInt("win");
            int draw = all.getInt("draw");
            int lose = all.getInt("lose");

            int points = row.getInt("points");

            JSONObject goals = all.getJSONObject("goals");

            int goalsFor = goals.getInt("for");
            int goalsAgainst = goals.getInt("against");

            int goalDiff = goalsFor - goalsAgainst;

            leagueStandingDao.upsert(
                    leagueId,
                    season,
                    teamId,
                    rank,
                    played,
                    win,
                    draw,
                    lose,
                    points,
                    goalsFor,
                    goalsAgainst,
                    goalDiff,
                    teamLogo   //  로고 전달
            );
        }
    }

    /* ==================================================
       메인보드 조회용
       ================================================== */
    public Object getStandingsForView(
            int leagueId,
            int season
    ) {
        return leagueStandingDao.findByLeagueAndSeason(
                leagueId,
                season
        );
    }
}
