package eplscout.service;

import eplscout.dao.ScoutRecommendationDao;
import eplscout.db.DBUtil;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;

@Service
public class ScoutRecommendationService {

    private final ScoutRecommendationDao recommendationDao;
    private final LLMService llmService;
    private final TeamSummaryService teamSummaryService;

    public ScoutRecommendationService(
            ScoutRecommendationDao recommendationDao,
            LLMService llmService,
            TeamSummaryService teamSummaryService
    ) {
        this.recommendationDao = recommendationDao;
        this.llmService = llmService;
        this.teamSummaryService = teamSummaryService;
    }

    /* =====================================================
       배치 추천 계산
       ===================================================== */
    public void recommendPlayers(long teamId, int season) {

        /* ===============================
           0. 팀 요약 정보 ( 여기 수정됨)
           =============================== */
        Map<String, Object> teamSummary =
                teamSummaryService.getTeamSummary(teamId, season);

        //  avgAge 안전 처리
        Object avgAgeObj = teamSummary.get("avgAge");
        double teamAvgAge =
                avgAgeObj == null
                        ? 0.0
                        : ((Number) avgAgeObj).doubleValue();

        //  weakPositions 안전 처리
        Object weakObj = teamSummary.get("weakPositions");
        Set<String> weakPositions;

        if (weakObj instanceof Set) {
            weakPositions = (Set<String>) weakObj;
        } else if (weakObj instanceof List) {
            weakPositions = new HashSet<>((List<String>) weakObj);
        } else {
            weakPositions = new HashSet<>();
        }

        boolean agingTeam = teamAvgAge >= 27.5;

        String sql = """
            SELECT
                p.player_id,
                p.name,
                p.position,
                p.age,
                pss.appearances,
                pss.minutes_played,
                pss.avg_rating,
                pss.goals,
                pss.assists,
                pss.shots,
                pss.key_passes,
                pss.pass_accuracy,
                pss.tackles,
                pss.interceptions,
                pss.clearances,
                pss.saves,
                pss.goals_conceded
            FROM player_season_stat pss
            JOIN player p ON pss.player_id = p.player_id
            WHERE pss.season = ?
              AND pss.team_id != ?
        """;

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, season);
            ps.setLong(2, teamId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {

                    long playerId = rs.getLong("player_id");
                    String playerName = rs.getString("name");

                    //  포지션 정규화
                    String rawPosition = rs.getString("position");
                    String position = normalizePosition(rawPosition);

                    int age = rs.getInt("age");
                    int apps = rs.getInt("appearances");
                    int minutes = rs.getInt("minutes_played");
                    double rating = rs.getDouble("avg_rating");

                    /* ===============================
                       1. 기본 실력 점수
                       =============================== */
                    double baseScore =
                            apps * 0.4
                          + (minutes / 90.0) * 0.3
                          + rating * 10 * 0.3;

                    double positionBonus =
                            PositionStatBonusCalculator.calculate(
                                    position,
                                    rs.getInt("goals"),
                                    rs.getInt("assists"),
                                    rs.getInt("shots"),
                                    rs.getInt("key_passes"),
                                    rs.getDouble("pass_accuracy"),
                                    rs.getInt("tackles"),
                                    rs.getInt("interceptions"),
                                    rs.getInt("clearances"),
                                    rs.getInt("saves"),
                                    rs.getInt("goals_conceded")
                            );

                    double finalScore = baseScore + positionBonus;

                    /* ===============================
                       2. 포지션 적합도 보정
                       =============================== */
                    if (weakPositions.contains(position)) {
                        finalScore *= 1.3;
                    } else {
                        finalScore *= 0.7;
                    }

                    /* ===============================
                       3. 연령 보정
                       =============================== */
                    if (age <= 24 && agingTeam) {
                        finalScore *= 1.1;
                    } else if (age >= 29) {
                        finalScore *= 0.9;
                    }

                    /* ===============================
                       4. 포텐셜 점수
                       =============================== */
                    double potentialScore =
                            (age <= 23 ? 10 :
                             age <= 26 ? 6 : 2)
                            + rating * 2;





                    if (agingTeam && age <= 24) {
                        potentialScore *= 1.2;
                    }



                    /* ===============================
                       5. 플레이 스타일 요약
                       =============================== */
                    String statSummary =
                            summarizePlayStyle(position, rs);

                    /* ===============================
                       6. LLM 설명
                       =============================== */
                    String styleDescription = "[AUTO] " + statSummary;


                    /* ===============================
                       7. DB 저장
                       =============================== */
                    recommendationDao.upsert(
                            teamId,
                            playerId,
                            season,
                            position,
                            finalScore,
                            potentialScore,
                            styleDescription
                    );
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("추천 계산 실패", e);
        }
    }

    /* =====================================================
       포지션 정규화
       ===================================================== */
    private String normalizePosition(String position) {
        return switch (position) {
            case "Goalkeeper" -> "GK";
            case "Defender"   -> "DF";
            case "Midfielder" -> "MF";
            case "Attacker"   -> "FW";
            default -> position;
        };
    }

    /* =====================================================
       플레이 스타일 요약
       ===================================================== */
    private String summarizePlayStyle(String position, ResultSet rs)
            throws SQLException {

        List<String> traits = new ArrayList<>();

        switch (position) {
            case "MF" -> {
                if (rs.getInt("key_passes") >= 40)
                    traits.add("찬스 메이킹 능력이 뛰어남");
                if (rs.getDouble("pass_accuracy") >= 85)
                    traits.add("빌드업 안정성과 패스 완성도가 높음");
                if (rs.getInt("shots") >= 30)
                    traits.add("중거리 슈팅과 공격 가담 능력 보유");
            }
            case "DF" -> {
                if (rs.getInt("tackles") + rs.getInt("interceptions") >= 60)
                    traits.add("적극적인 압박과 볼 탈취 능력");
                if (rs.getInt("clearances") >= 80)
                    traits.add("박스 안 수비와 위기 대응에 강점");
            }
            case "FW" -> {
                if (rs.getInt("goals") >= 10)
                    traits.add("페널티 박스 내 결정력 우수");
                if (rs.getInt("assists") >= 7)
                    traits.add("연계 플레이에 적극적인 공격수");
            }
            case "GK" -> {
                if (rs.getInt("saves") >= 70)
                    traits.add("선방 능력이 뛰어난 골키퍼");
                if (rs.getInt("goals_conceded") <= 40)
                    traits.add("안정적인 경기 운영이 가능한 수문장");
            }
        }

        return traits.isEmpty()
                ? "전반적인 밸런스가 좋은 선수"
                : String.join(", ", traits);
    }

    /* =====================================================
       GUI 조회
       ===================================================== */
    public Map<String, Object> view(long teamId, int season) {
        Map<String, Object> result = new HashMap<>();
        result.put(
                "recommendations",
                recommendationDao.findForView(teamId, season)
        );
        return result;
    }

    /* =====================================================
       추천 사유
       ===================================================== */
    public String generateRecommendationReason(
            String teamName,
            String playerName,
            String position,
            double score,
            double potentialScore
    ) {
        return llmService.generateRecommendationReason(
                teamName,
                playerName,
                position,
                score,
                potentialScore,
                "팀 전력 구조와 연령을 고려한 추천"
        );
    }
}
