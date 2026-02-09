package eplscout.service;

/**
 * PositionStatBonusCalculator
 *
 * 책임
 * - 포지션별 시즌 누적 스탯을 기반으로
 *   "보정 점수(position bonus)" 계산
 *
 * 설계 원칙
 * - baseScore(출전/평점 기반 점수)는 절대 건드리지 않음
 * - 포지션 수행 능력 차이만 반영
 * - mock 데이터 폭주 방지
 * - null-safe / 0-safe 계산
 */
public class PositionStatBonusCalculator {

    public static double calculate(
            String position,

            // ---------- attack ----------
            int goals,
            int assists,
            int shots,

            // ---------- pass ----------
            int keyPasses,
            double passAccuracy,

            // ---------- defense ----------
            int tackles,
            int interceptions,
            int clearances,

            // ---------- goalkeeper ----------
            int saves,
            int goalsConceded
    ) {

        if (position == null) return 0;

        return switch (position) {

            // =============================
            // Attacker (기존 유지)
            // =============================
            case "Attacker" -> {
                yield goals * 0.5
                     + assists * 0.3
                     + shots * 0.1;
            }

            // =============================
            // Midfielder (기존 유지)
            // =============================
            case "Midfielder" -> {
                yield assists * 0.4
                     + keyPasses * 0.3
                     + passAccuracy * 0.05;
            }

            // =============================
            // Defender (clearances 보정)
            // =============================
            case "Defender" -> {

                double tackleScore = tackles * 0.3;
                double interceptionScore = interceptions * 0.04;

                /**
                 * clearances 처리 방식
                 * - 로그 스케일 적용
                 * - 상한선 적용 (mock 데이터 폭주 방지)
                 */
                double clearanceScore =
                        Math.log(clearances + 1) * 3.0;

                clearanceScore = Math.min(clearanceScore, 8.0);

                yield tackleScore
                     + interceptionScore
                     + clearanceScore;
            }

            // =============================
            // Goalkeeper (saves 보정)
            // =============================
            case "Goalkeeper" -> {

                /**
                 * saves 처리 방식
                 * - 로그 스케일 적용
                 * - 상한선 적용
                 */
                double saveScore =
                        Math.log(saves + 1) * 3.0;

                saveScore = Math.min(saveScore, 10.0);

                double concedePenalty =
                        goalsConceded * 0.3;

                yield saveScore - concedePenalty;
            }

            default -> 0;
        };
    }
}
