package eplscout.model;

/**
 * RosterEntry
 * - team_roster 테이블에 들어갈 한 줄(한 선수의 소속 정보) 표현
 * - player_id/team_id 같은 "내부 FK"는 DB 조회 후 채워진다.
 */
public class RosterEntry {

    private long teamId;      // 내부 team.team_id
    private int season;       // 예: 2023

    private long playerId;    // 내부 player.player_id
    private int apiPlayerId;  // 외부 player.id (매핑/검증용)

    private int squadNumber;  // 등번호(API number)
    private String positionSnapshot; // 스쿼드 당시 포지션(API position)

    public RosterEntry() {}

    public RosterEntry(long teamId, int season, long playerId, int apiPlayerId, int squadNumber, String positionSnapshot) {
        this.teamId = teamId;
        this.season = season;
        this.playerId = playerId;
        this.apiPlayerId = apiPlayerId;
        this.squadNumber = squadNumber;
        this.positionSnapshot = positionSnapshot;
    }

    public long getTeamId() {
        return teamId;
    }

    public void setTeamId(long teamId) {
        this.teamId = teamId;
    }

    public int getSeason() {
        return season;
    }

    public void setSeason(int season) {
        this.season = season;
    }

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public int getApiPlayerId() {
        return apiPlayerId;
    }

    public void setApiPlayerId(int apiPlayerId) {
        this.apiPlayerId = apiPlayerId;
    }

    public int getSquadNumber() {
        return squadNumber;
    }

    public void setSquadNumber(int squadNumber) {
        this.squadNumber = squadNumber;
    }

    public String getPositionSnapshot() {
        return positionSnapshot;
    }

    public void setPositionSnapshot(String positionSnapshot) {
        this.positionSnapshot = positionSnapshot;
    }
}
