package model;

public class Team {

    private int apiTeamId;   // API에서 내려준 팀 고유 ID
    private String name;
    private String country;
    private int founded;

    public Team(int apiTeamId, String name, String country, int founded) {
        this.apiTeamId = apiTeamId;
        this.name = name;
        this.country = country;
        this.founded = founded;
    }

    public int getApiTeamId() {
        return apiTeamId;
    }

    public String getName() {
        return name;
    }
}
