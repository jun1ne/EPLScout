package eplscout.service;

import eplscout.dao.TeamDao;
import eplscout.dao.PlayerDao;

import java.util.HashMap;
import java.util.Map;

public class TeamManagementService {

    private final TeamDao teamDao = new TeamDao();
    private final PlayerDao playerDao = new PlayerDao();

    public Map<String, Object> getTeamDetail(int teamId, int season) {

        Map<String, Object> result = new HashMap<>();

        // 팀 기본 정보
        result.put("team", teamDao.findTeamById(teamId));

        // 포지션별 선수 수
        result.put(
                "positionCounts",
                playerDao.countPlayersByPosition(teamId, season)
        );

        // 평균 연령 / 평점
        result.put(
                "avgStat",
                playerDao.findTeamAverageStat(teamId, season)
        );

        // 부족 포지션
        result.put(
                "weakPosition",
                playerDao.findWeakPosition(teamId, season)
        );

        return result;
    }
}
