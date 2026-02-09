package eplscout.service;

import eplscout.dao.TeamDao;
import eplscout.model.Team;

import java.util.List;
import java.util.Map;

/**
 * TeamService
 *
 * 역할
 * 1) 외부 API → DB 적재 (배치용)
 * 2) GUI용 팀 목록 제공 (조회 전용)
 * 3)  추천 사유 생성 (LLM 연동)
 *
 * ※ 비즈니스 로직은 여기,
 * ※ SQL은 TeamDao,
 * ※ API 통신은 TeamApiService / LLMService
 */
public class TeamService {

    private final TeamApiService teamApiService = new TeamApiService();
    private final TeamDao teamDao = new TeamDao();

    //  LLM 서비스 추가
    private final LLMService llmService = new LLMService();

    /* =====================================================
       1. 배치용: EPL 팀 목록 API → DB 저장
       ===================================================== */

    /**
     * EPL 팀 목록을 외부 API에서 받아 DB에 저장
     * - 초기 세팅
     * - 시즌 데이터 재적재 시 사용
     */
    public void fetchAndSaveEplTeams() throws Exception {
        List<Team> teams = teamApiService.getEplTeams();
        teamDao.upsertTeams(teams);
    }

    /* =====================================================
       2. GUI용: 시즌별 팀 목록 조회
       ===================================================== */

    /**
     * 웹 GUI용 팀 목록 조회
     *
     * 반환 형태:
     * [
     *   { id: team_id, name: "Arsenal" },
     *   { id: team_id, name: "Liverpool" },
     *   ...
     * ]
     */
    public List<Map<String, Object>> getTeamsForView(int season) {
        return teamDao.findTeamsForView(season);
    }

    /* =====================================================
       3.  GUI용: 추천 사유 생성 (LLM)
       ===================================================== */

    /**
     * 추천 선수에 대한 자연어 추천 사유 생성
     *
     *  주의
     * - 추천 대상은 이미 시스템에서 결정됨
     * - LLM은 "설명"만 담당
     */
    public String generateRecommendationReason(
            String teamName,
            String playerName,
            String position,
            double score
    ) {

        // 규칙 기반 추천 사유 (현재는 고정 문장)
        // 나중에 ScoutService 로직과 연결 가능
        String ruleReason =
                "팀의 해당 포지션 전력이 부족하여 즉시 전력 보강이 필요함";

        // 포텐셜 점수 (임시 계산)
        double potentialScore = score * 0.2;

        return llmService.generateRecommendationReason(
                teamName,
                playerName,
                position,
                score,
                potentialScore,
                ruleReason
        );
    }
}
