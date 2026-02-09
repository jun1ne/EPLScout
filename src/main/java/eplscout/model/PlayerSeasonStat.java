package eplscout.model;

/**
 * PlayerSeasonStat (Model)
 * - 선수의 시즌 누적 스탯(집계 결과)을 담는 객체
 * - 추천 점수 계산, 선수 리스트 정렬(평점/출전시간 등)에 직접 사용
 */
public class PlayerSeasonStat {

    private long playerId;  // 내부 FK (player.player_id)
    private long teamId;    // 내부 FK (team.team_id)

    private int leagueId;   // EPL=39
    private int season;     // 예: 2023

    private String position;
    private int appearances;
    private int minutesPlayed;
    private int goals;
    private int assists;

    private Double rating; // null 가능

    public PlayerSeasonStat() {}

    public long getPlayerId() { return playerId; }
    public void setPlayerId(long playerId) { this.playerId = playerId; }

    public long getTeamId() { return teamId; }
    public void setTeamId(long teamId) { this.teamId = teamId; }

    public int getLeagueId() { return leagueId; }
    public void setLeagueId(int leagueId) { this.leagueId = leagueId; }

    public int getSeason() { return season; }
    public void setSeason(int season) { this.season = season; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public int getAppearances() { return appearances; }
    public void setAppearances(int appearances) { this.appearances = appearances; }

    public int getMinutesPlayed() { return minutesPlayed; }
    public void setMinutesPlayed(int minutesPlayed) { this.minutesPlayed = minutesPlayed; }

    public int getGoals() { return goals; }
    public void setGoals(int goals) { this.goals = goals; }

    public int getAssists() { return assists; }
    public void setAssists(int assists) { this.assists = assists; }

    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }
}
