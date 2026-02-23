package eplscout.scoring;

import eplscout.model.PlayerSeasonStat;

/**
 * PerformanceCalculator
 *
 * 역할:
 * - 선수 시즌 퍼포먼스 점수 계산
 * - 포지션별 가중치 적용
 * - 단위 정규화 적용 (MF 독식 방지)
 */
public class PerformanceCalculator {

    public double calculate(PlayerSeasonStat stat) {

        return switch (stat.getPosition()) {
            case "FW" -> calculateFW(stat);
            case "MF" -> calculateMF(stat);
            case "DF" -> calculateDF(stat);
            case "GK" -> calculateGK(stat);
            default -> 0.0;
        };
    }

    /* ================= FW ================= */
    private double calculateFW(PlayerSeasonStat s) {

        double goalsScore = s.getGoals() / 20.0;
        double assistScore = s.getAssists() / 15.0;
        double shotScore = s.getShots() / 80.0;
        double ratingScore = safe(s.getRating()) / 10.0;
        double minutesScore = calculateMinutesRatio(s);

        return (goalsScore * 0.35)
             + (assistScore * 0.2)
             + (shotScore * 0.15)
             + (ratingScore * 0.2)
             + (minutesScore * 0.1);
    }

    /* ================= MF ================= */
    private double calculateMF(PlayerSeasonStat s) {

        double keyPassScore = s.getKeyPasses() / 80.0;
        double passAccuracyScore = safe(s.getPassAccuracy()) / 100.0;
        double tackleScore = s.getTackles() / 100.0;
        double ratingScore = safe(s.getRating()) / 10.0;
        double minutesScore = calculateMinutesRatio(s);

        return (keyPassScore * 0.25)
             + (passAccuracyScore * 0.2)
             + (tackleScore * 0.2)
             + (ratingScore * 0.2)
             + (minutesScore * 0.15);
    }

    /* ================= DF ================= */
    private double calculateDF(PlayerSeasonStat s) {

        double tackleScore = s.getTackles() / 120.0;
        double interceptionScore = s.getInterceptions() / 80.0;
        double clearanceScore = s.getClearances() / 200.0;
        double ratingScore = safe(s.getRating()) / 10.0;

        return (tackleScore * 0.3)
             + (interceptionScore * 0.2)
             + (clearanceScore * 0.2)
             + (ratingScore * 0.3);
    }

    /* ================= GK ================= */
    private double calculateGK(PlayerSeasonStat s) {

        double savesScore = Math.log(s.getSaves() + 1) / 5.0;  // 폭주 방지
        double ratingScore = safe(s.getRating()) / 10.0;
        double minutesScore = calculateMinutesRatio(s);
        double concededPenalty = s.getGoalsConceded() * 0.02;

        return (savesScore * 0.4)
             + (ratingScore * 0.3)
             + (minutesScore * 0.3)
             - concededPenalty;
    }

    private double calculateMinutesRatio(PlayerSeasonStat s) {
        return s.getMinutesPlayed() / (38.0 * 90.0);
    }

    private double safe(Double value) {
        return value == null ? 0.0 : value;
    }
}
