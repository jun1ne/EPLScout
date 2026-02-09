package eplscout.service;

import eplscout.dao.TeamPositionStatDao;

import java.util.HashMap;
import java.util.Map;

/**
 * DashboardService
 *
 * 메인 대시보드 전용 서비스
 *
 * 현재 설계 기준
 * - 리그 순위표: LeagueStandingService에서 직접 제공

 * - 대시보드 알림: 포지션 부족 팀만 표시
 */
public class DashboardService {

    // 팀 포지션 통계 DAO
    private final TeamPositionStatDao positionDao =
            new TeamPositionStatDao();

    /**
     * 메인 대시보드 요약 데이터
     *
     * 반환 구조 (프론트 호환 유지)
     * {
     *   positionAlerts: [...]
     * }
     */
    public Map<String, Object> getDashboardSummary(int season) {

        Map<String, Object> result = new HashMap<>();

        /**
         * - 불필요한 DAO 호출 방지
         */

        // 포지션 부족 팀 알림
        result.put(
                "positionAlerts",
                positionDao.findTeamsWithWeakPositions(season)
        );

        return result;
    }
}
