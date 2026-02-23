package eplscout.scoring;

import eplscout.model.Player;
import eplscout.model.PlayerSeasonStat;

import java.time.LocalDate;
import java.time.Period;

/**
 * PlayerValueCalculator
 *
 * 역할:
 * - 기존 Performance 점수 기반
 * - 나이 보정
 * - 팀 레벨 매칭 보정 (곱셈 기반 + 강제 컷)
 * - 부상 리스크 반영
 *
 * 최종 선수 가치 점수 산출
 */
public class PlayerValueCalculator {

    private final PerformanceCalculator performanceCalculator =
            new PerformanceCalculator();

    /**
     * 최종 선수 가치 계산
     *
     * @param stat       시즌 스탯
     * @param player     선수 기본 정보
     * @param teamLevel  팀 평균 평점
     * @param injuryRate 부상률 (없으면 0)
     */
    public double calculateValue(
            PlayerSeasonStat stat,
            Player player,
            double teamLevel,
            double injuryRate
    ) {

        double performanceScore = performanceCalculator.calculate(stat);

        double ageBonus = calculateAgeBonus(player);

        double levelFactor = calculateLevelFactor(
                safe(stat.getRating()),
                teamLevel
        );

        // 강제 컷으로 제외된 경우
        if (levelFactor == 0.0) {
            return 0.0;
        }

        // 부상 리스크는 곱셈 감점 방식
        double injuryFactor = 1 - (injuryRate * 0.3);

        if (injuryFactor < 0.7) {
            injuryFactor = 0.7;   // 최소 보정선
        }

        return (performanceScore * levelFactor * injuryFactor)
                + ageBonus;
    }

    /* =============================== */
    /* 나이 보정 */
    /* =============================== */

    private double calculateAgeBonus(Player player) {

        int age = getRealAge(player);

        if (age <= 23) return 0.08;
        if (age <= 28) return 0.03;
        if (age <= 32) return 0.0;
        return -0.05;
    }

    private int getRealAge(Player player) {

        if (player.getBirthDate() != null) {
            return Period.between(
                    player.getBirthDate(),
                    LocalDate.now()
            ).getYears();
        }

        return player.getAge();
    }

    /* =============================== */
    /* 팀 레벨 매칭 보정 ( 강제 컷 포함) */
    /* =============================== */

    private double calculateLevelFactor(
            double playerRating,
            double teamLevel
    ) {

        double gap = playerRating - teamLevel;

        //  너무 높은 선수는 현실성 부족 → 추천 제외
        if (gap > 1.2) {
            return 0.0;
        }

        // 강한 감점
        if (gap > 1.0) return 0.6;

        // 중간 감점
        if (gap > 0.6) return 0.75;

        // 팀 수준과 거의 동일 → 보너스
        if (Math.abs(gap) <= 0.4) return 1.15;

        // 너무 낮은 선수
        if (gap < -0.8) return 0.7;

        // 기본 약한 감점
        return 0.9;
    }

    private double safe(Double v) {
        return v == null ? 0.0 : v;
    }
}
