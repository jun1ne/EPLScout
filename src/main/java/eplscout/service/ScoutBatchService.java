package eplscout.service;

import eplscout.dao.TeamDao;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScoutBatchService {

    private final TeamDao teamDao;
    private final ScoutRecommendationService scoutRecommendationService;

    public ScoutBatchService(
            TeamDao teamDao,
            ScoutRecommendationService scoutRecommendationService
    ) {
        this.teamDao = teamDao;
        this.scoutRecommendationService = scoutRecommendationService;
    }

    /**
     * 시즌 기준 전체 팀 추천 배치 실행
     */
    public void runRecommendationBatch(int season) {

        try {
            //  season 기준 team 테이블에 존재하는 team_id 목록           
            List<Integer> teamIds = teamDao.findApiTeamIdsBySeason(season);

            for (int teamId : teamIds) {

                System.out.println(
                        "추천 배치 실행: team_id=" + teamId + ", season=" + season
                );

                scoutRecommendationService.recommendPlayers(
                        teamId,
                        season
                );
            }

        } catch (Exception e) {
            throw new RuntimeException("추천 배치 실행 실패", e);
        }
    }
}
