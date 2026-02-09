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
 * - DB 적재
 * - 메인 대시보드 조회용 데이터 제공
 */
@Service
public class LeagueStandingService {

    private final LeagueStandingDao leagueStandingDao;
    private final TeamDao teamDao;

    // 외부 API 호출 전담
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

            // 순위
            int rank = Integer.parseInt(
                    row.get("rank").toString()
            );

            // 팀 정보
            JSONObject team = row.getJSONObject("team");
            int apiTeamId = Integer.parseInt(
                    team.get("id").toString()
            );

            long teamId =
                    teamDao.findTeamIdByApiTeamIdAndSeason(apiTeamId, season);

            // 내부 팀 없으면 스킵
            if (teamId == 0L) continue;

            // 전체 성적
            JSONObject all = row.getJSONObject("all");

            int played = Integer.parseInt(
                    all.get("played").toString()
            );
            int win = Integer.parseInt(
                    all.get("win").toString()
            );
            int draw = Integer.parseInt(
                    all.get("draw").toString()
            );
            int lose = Integer.parseInt(
                    all.get("lose").toString()
            );

            int points = Integer.parseInt(
                    row.get("points").toString()
            );

            JSONObject goals = all.getJSONObject("goals");

            int goalsFor = Integer.parseInt(
                    goals.get("for").toString()
            );
            int goalsAgainst = Integer.parseInt(
                    goals.get("against").toString()
            );

            int goalDiff = goalsFor - goalsAgainst;

            // DB 저장
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
                    goalDiff
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
