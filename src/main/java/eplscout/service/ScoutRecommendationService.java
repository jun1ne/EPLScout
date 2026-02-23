package eplscout.service;

import eplscout.dao.ScoutRecommendationDao;
import eplscout.db.DBUtil;
import eplscout.model.PlayerSeasonStat;
import eplscout.model.PlayerInjuryStat;
import eplscout.model.Player;
import eplscout.scoring.PerformanceCalculator;
import eplscout.scoring.InjuryRiskCalculator;
import eplscout.scoring.PlayerValueCalculator;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;

/**
 * ScoutRecommendationService
 *
 * 핵심 책임:
 * 1. 시즌 기준 외부 선수 풀 조회
 * 2. 퍼포먼스 점수 계산
 * 3. 부상 리스크 반영
 * 4. 팀 구조(약점/연령) 보정
 * 5. 최종 추천 점수 및 포텐셜 산출
 * 6. scout_recommendation 테이블 저장
 */
@Service
public class ScoutRecommendationService {

    private final ScoutRecommendationDao recommendationDao;
    private final LLMService llmService;
    private final TeamSummaryService teamSummaryService;

    /* ===============================
       계산 전용 객체 (점수 엔진)
       =============================== */

    private final PerformanceCalculator performanceCalculator =
            new PerformanceCalculator();

    private final InjuryRiskCalculator injuryRiskCalculator =
            new InjuryRiskCalculator();

    private final PlayerValueCalculator playerValueCalculator =
            new PlayerValueCalculator();

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
       배치 추천 계산 (부상 고급 반영 버전)
       ===================================================== */
    public void recommendPlayers(long teamId, int season, String mode) {

        Map<String, Object> teamSummary =
                teamSummaryService.getTeamSummary(teamId, season);

        double teamAvgAge = teamSummary.get("avgAge") == null
                ? 0.0
                : ((Number) teamSummary.get("avgAge")).doubleValue();

        double teamLevel = teamSummary.get("avgRating") == null
                ? 0.0
                : ((Number) teamSummary.get("avgRating")).doubleValue();

        boolean agingTeam = teamAvgAge >= 27.5;

        // 기존 추천 결과 삭제
        recommendationDao.deleteByTeamAndSeason(teamId, season);

        List<String> weakPositions =
                (List<String>) teamSummary.get("weakPositions");

        if (weakPositions == null || weakPositions.isEmpty()) {
            return;
        }

        List<String> normalizedWeakPositions = new ArrayList<>();
        for (String pos : weakPositions) {
            normalizedWeakPositions.add(normalizePosition(pos));
        }

        String sql =
                "SELECT " +
                "p.player_id, " +
                "p.name, " +
                "p.position, " +
                "p.age, " +
                "p.birth_date, " +
                "pss.team_id, " +
                "pss.appearances, " +
                "pss.minutes_played, " +
                "pss.avg_rating, " +
                "pss.goals, " +
                "pss.assists, " +
                "pss.shots, " +
                "pss.key_passes, " +
                "pss.pass_accuracy, " +
                "pss.tackles, " +
                "pss.interceptions, " +
                "pss.clearances, " +
                "pss.saves, " +
                "pss.goals_conceded " +
                "FROM player_season_stat pss " +
                "JOIN player p ON pss.player_id = p.player_id " +
                "WHERE pss.season = ? " +
                "AND pss.team_id != ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, season);
            ps.setLong(2, teamId);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    PlayerSeasonStat stat =
                            buildPlayerSeasonStat(rs, season);

                    if (!normalizedWeakPositions.contains(stat.getPosition())) {
                        continue;
                    }

                    Player player = new Player(
                            rs.getInt("player_id"),
                            rs.getString("name"),
                            rs.getInt("age"),
                            0,
                            rs.getString("position"),
                            null
                    );

                    player.setName(rs.getString("name"));
                    player.setPosition(rs.getString("position"));
                    player.setAge(rs.getInt("age"));

                    java.sql.Date birth = rs.getDate("birth_date");

                    if (birth != null) {
                        player.setBirthDate(birth.toLocalDate());
                    }

                    PlayerInjuryStat injuryStat =
                            loadInjuryStat(conn,
                                    stat.getPlayerId(),
                                    season);

                    if (injuryStat != null && injuryStat.isCurrentInjured()) {
                        continue;
                    }

                    double injuryRate =
                            injuryStat == null ? 0.0 :
                                    injuryStat.getInjuryRate();

                    /* ===============================
                        Market Value 통일 계산
                       =============================== */
                    double playerValue =
                            playerValueCalculator.calculateValue(
                                    stat,
                                    player,
                                    stat.getRating(),
                                    injuryRate
                            );

                    // 100점 스케일 변환
                    playerValue = Math.min(playerValue * 100, 100);

                    double potentialScore =
                            calculatePotential(
                                    player.getAge(),
                                    stat.getRating(),
                                    agingTeam,
                                    injuryStat
                            );

                    double finalScore =
                            (playerValue * 0.7)
                          + (potentialScore * 5 * 0.3);

                    finalScore *= 1.2;

                    String styleDescription =
                            "[AUTO] " + summarizePlayStyle(stat);

                    recommendationDao.upsert(
                            conn,
                            teamId,
                            stat.getPlayerId(),
                            season,
                            stat.getPosition(),
                            finalScore,
                            potentialScore,
                            playerValue,
                            styleDescription
                    );
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("추천 계산 실패", e);
        }
    }

    private double calculatePotential(
            int age,
            Double rating,
            boolean agingTeam,
            PlayerInjuryStat injuryStat
    ) {

        /*
         * 성장 잠재력은 경기력과 분리하여
         * "연령 기반 성장 가능성 지표"로 설계
         */

        double base;

        if (age <= 21) base = 10;
        else if (age <= 24) base = 8;
        else if (age <= 27) base = 5;
        else if (age <= 30) base = 2;
        else base = 0.5;

        double result = base;

        // 고령 팀일 경우 젊은 선수 보정
        if (agingTeam && age <= 24) {
            result *= 1.2;
        }

        // 최근 3년 부상 이력 많으면 감점
        if (injuryStat != null &&
                injuryStat.getTotalInjuryLast3Years() >= 5) {
            result *= 0.75;
        }

        return result;
    }

    private PlayerInjuryStat loadInjuryStat(
            Connection conn,
            long playerId,
            int season) throws SQLException {

        String sql = """
            SELECT *
            FROM player_injury_stat
            WHERE player_id = ?
              AND season = ?
        """;

        try (PreparedStatement ps =
                     conn.prepareStatement(sql)) {

            ps.setLong(1, playerId);
            ps.setInt(2, season);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {

                    PlayerInjuryStat stat =
                            new PlayerInjuryStat();

                    stat.setPlayerId(playerId);
                    stat.setSeason(season);
                    stat.setInjuryGames(
                            rs.getInt("injury_games"));
                    stat.setInjuryRate(
                            rs.getDouble("injury_rate"));
                    stat.setRepeatInjuryCount(
                            rs.getInt("repeat_injury_count"));
                    stat.setTotalInjuryLast3Years(
                            rs.getInt("total_injury_last3years"));
                    stat.setCurrentInjured(
                            rs.getInt("current_injured") == 1);

                    return stat;
                }
            }
        }

        return null;
    }

    private PlayerSeasonStat loadSeasonStatForLLM(
            String playerName,
            int season) {

        String sql = """
            SELECT *
            FROM player_season_stat pss
            JOIN player p ON p.player_id = pss.player_id
            WHERE p.name = ?
              AND pss.season = ?
            LIMIT 1
        """;

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, playerName);
            ps.setInt(2, season);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    return buildPlayerSeasonStat(rs, season);
                }
            }

        } catch (Exception ignored) {
        }

        return null;
    }

    private PlayerSeasonStat buildPlayerSeasonStat(
            ResultSet rs,
            int season) throws SQLException {

        PlayerSeasonStat stat = new PlayerSeasonStat();

        stat.setPlayerId(rs.getLong("player_id"));
        stat.setTeamId(rs.getLong("team_id"));
        stat.setSeason(season);
        stat.setPosition(normalizePosition(
                rs.getString("position")));

        stat.setAppearances(rs.getInt("appearances"));
        stat.setMinutesPlayed(rs.getInt("minutes_played"));
        stat.setRating(rs.getDouble("avg_rating"));

        stat.setGoals(rs.getInt("goals"));
        stat.setAssists(rs.getInt("assists"));
        stat.setShots(rs.getInt("shots"));
        stat.setKeyPasses(rs.getInt("key_passes"));
        stat.setPassAccuracy(rs.getDouble("pass_accuracy"));

        stat.setTackles(rs.getInt("tackles"));
        stat.setInterceptions(rs.getInt("interceptions"));
        stat.setClearances(rs.getInt("clearances"));

        stat.setSaves(rs.getInt("saves"));
        stat.setGoalsConceded(rs.getInt("goals_conceded"));

        return stat;
    }

    private String normalizePosition(String position) {
        return switch (position) {
            case "Goalkeeper" -> "GK";
            case "Defender" -> "DF";
            case "Midfielder" -> "MF";
            case "Attacker" -> "FW";
            default -> position;
        };
    }

    private String summarizePlayStyle(PlayerSeasonStat stat) {
        return "전반적인 밸런스가 좋은 선수";
    }

    public Map<String, Object> view(long teamId, int season) {

        recommendPlayers(teamId, season, "NORMAL");

        Map<String, Object> result = new HashMap<>();

        result.put(
                "recommendations",
                recommendationDao.findForView(teamId, season)
        );

        return result;
    }

    public String generateRecommendationReason(
            String teamName,
            String playerName,
            String position,
            double score,
            double potentialScore
    ) {

        int season = 2023; // 현재 시스템 기본 시즌

        PlayerSeasonStat stat =
                loadSeasonStatForLLM(playerName, season);

        if (stat == null) {
            return llmService.generateRecommendationReason(
                    teamName,
                    playerName,
                    position,
                    score,
                    potentialScore,
                    "팀 전력 구조와 연령을 고려한 추천"
            );
        }

        return llmService.generateAdvancedRecommendationReason(
                teamName,
                playerName,
                position,
                stat.getGoals(),
                stat.getAssists(),
                stat.getRating(),
                stat.getAppearances(),
                score,
                potentialScore
        );
    }

    public void recommendPlayers(long teamId, int season) {
        recommendPlayers(teamId, season, "NORMAL");
    }
}
