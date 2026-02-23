package eplscout.model;

/**
 * PlayerSeasonStat
 *
 * 목적:
 * - DB에서 읽어온 시즌 누적 데이터를 하나의 객체로 보관
 * - 모든 점수 계산은 이 객체를 기반으로 수행
 *
 * 설계 원칙:
 * - 계산에 필요한 모든 필드는 모델에 포함
 * - ResultSet 직접 사용 금지 → 유지보수성 향상
 */
public class PlayerSeasonStat {

    /* ===============================
       기본 식별 정보
       =============================== */
    private long playerId;
    private long teamId;
    private int season;
    private String position;

    /* ===============================
       출전 관련
       =============================== */
    private int appearances;
    private int minutesPlayed;
    private Double rating;

    /* ===============================
       공격 지표
       =============================== */
    private int goals;
    private int assists;
    private int shots;
    private int keyPasses;

    /* ===============================
       패스
       =============================== */
    private Double passAccuracy;

    /* ===============================
       수비
       =============================== */
    private int tackles;
    private int interceptions;
    private int clearances;

    /* ===============================
       골키퍼 전용
       =============================== */
    private int saves;
    private int goalsConceded;

    /* ===============================
       Getter / Setter
       =============================== */

    public long getPlayerId() { return playerId; }
    public void setPlayerId(long playerId) { this.playerId = playerId; }

    public long getTeamId() { return teamId; }
    public void setTeamId(long teamId) { this.teamId = teamId; }

    public int getSeason() { return season; }
    public void setSeason(int season) { this.season = season; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public int getAppearances() { return appearances; }
    public void setAppearances(int appearances) { this.appearances = appearances; }

    public int getMinutesPlayed() { return minutesPlayed; }
    public void setMinutesPlayed(int minutesPlayed) { this.minutesPlayed = minutesPlayed; }

    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }

    public int getGoals() { return goals; }
    public void setGoals(int goals) { this.goals = goals; }

    public int getAssists() { return assists; }
    public void setAssists(int assists) { this.assists = assists; }

    public int getShots() { return shots; }
    public void setShots(int shots) { this.shots = shots; }

    public int getKeyPasses() { return keyPasses; }
    public void setKeyPasses(int keyPasses) { this.keyPasses = keyPasses; }

    public Double getPassAccuracy() { return passAccuracy; }
    public void setPassAccuracy(Double passAccuracy) { this.passAccuracy = passAccuracy; }

    public int getTackles() { return tackles; }
    public void setTackles(int tackles) { this.tackles = tackles; }

    public int getInterceptions() { return interceptions; }
    public void setInterceptions(int interceptions) { this.interceptions = interceptions; }

    public int getClearances() { return clearances; }
    public void setClearances(int clearances) { this.clearances = clearances; }

    public int getSaves() { return saves; }
    public void setSaves(int saves) { this.saves = saves; }

    public int getGoalsConceded() { return goalsConceded; }
    public void setGoalsConceded(int goalsConceded) { this.goalsConceded = goalsConceded; }
}
