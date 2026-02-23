package eplscout.scoring;

import eplscout.model.PlayerInjuryStat;

/**
 * InjuryRiskCalculator
 *
 * 최근 3년 부상 이력 기반 리스크 점수 계산
 */
public class InjuryRiskCalculator {

    public double calculate(PlayerInjuryStat stat) {

        double risk = 0;

        // 현재 부상
        if (stat.isCurrentInjured()) {
            risk += 35;
        }

        // 시즌 결장률
        risk += stat.getInjuryRate() * 30;

        // 반복 부상
        risk += stat.getRepeatInjuryCount() * 10;

        // 최근 3년 누적
        risk += stat.getTotalInjuryLast3Years() * 5;

        return risk;
    }
}
