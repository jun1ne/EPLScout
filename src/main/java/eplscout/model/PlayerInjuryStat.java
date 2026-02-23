package eplscout.model;

/**
 * PlayerInjuryStat
 *
 *  player_injury_stat 테이블 매핑 모델
 *  시즌 기준 부상 통계 집계 결과 저장용
 *  추천 점수 계산 시 InjuryRiskCalculator에서 사용
 */
public class PlayerInjuryStat {

    private long playerId;                 // 선수 ID
    private int season;                    // 시즌

    private int injuryGames;               // 해당 시즌 결장 경기 수
    private double injuryRate;             // 결장률 (결장 경기 / 전체 경기)

    private int repeatInjuryCount;         // 동일 유형 반복 부상 횟수
    private int totalInjuryLast3Years;     // 최근 3년 총 부상 수

    private boolean currentInjured;        // 현재 부상 여부

    /* ===============================
       Getter / Setter
       =============================== */

    public long getPlayerId() { return playerId; }
    public void setPlayerId(long playerId) { this.playerId = playerId; }

    public int getSeason() { return season; }
    public void setSeason(int season) { this.season = season; }

    public int getInjuryGames() { return injuryGames; }
    public void setInjuryGames(int injuryGames) { this.injuryGames = injuryGames; }

    public double getInjuryRate() { return injuryRate; }
    public void setInjuryRate(double injuryRate) { this.injuryRate = injuryRate; }

    public int getRepeatInjuryCount() { return repeatInjuryCount; }
    public void setRepeatInjuryCount(int repeatInjuryCount) { this.repeatInjuryCount = repeatInjuryCount; }

    public int getTotalInjuryLast3Years() { return totalInjuryLast3Years; }
    public void setTotalInjuryLast3Years(int totalInjuryLast3Years) { this.totalInjuryLast3Years = totalInjuryLast3Years; }

    public boolean isCurrentInjured() { return currentInjured; }
    public void setCurrentInjured(boolean currentInjured) { this.currentInjured = currentInjured; }
}
