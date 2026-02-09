package eplscout.model;

/**
 * Team (Model / Entity)
 * - 팀의 데이터만 담는 객체 (DTO/VO 역할)
 * - 외부 API(JSON)에서 가져온 값을 시스템에서 쓰기 좋은 형태로 보관
 *
 * 설계 포인트:
 * - API 응답(JSON 구조)이 바뀌어도 Team만 유지하면
 *   DB 저장(DAO)/추천 로직/화면 출력 코드는 덜 영향을 받음
 * - 외부 데이터를 내부 표준 데이터로 변환하는 "중간 형태" 역할
 */
public class Team {

    // ==================================================
    // 0) 내부 DB 식별자 
    // ==================================================

    /**
     * teamId
     * - DB team 테이블의 PK (AUTO_INCREMENT)
     * - 내부 로직/배치/조인/조회용
     *
     * ※ 외부 API에는 없음
     * ※ DB에서 조회할 때만 세팅됨
     */
    private long teamId;

    // ==================================================
    // 1) 외부 API 식별자 / 필수 정보
    // ==================================================

    /**
     * apiTeamId
     * - API-Football에서 내려주는 팀 고유 ID (외부 식별자)
     * - DB에서 UNIQUE (api_team_id, season)
     */
    private int apiTeamId;

    /** 팀 이름 (예: Manchester City) */
    private String name;

    /** 국가 (예: England) */
    private String country;

    /** 창단 연도 */
    private int foundedYear;

    // ==================================================
    // 2) UI / 부가 정보
    // ==================================================

    /** 팀 로고 URL */
    private String logoUrl;

    /** 홈 구장 이름 */
    private String stadiumName;

    /** 구장 도시 */
    private String stadiumCity;

    /** 구장 수용 인원 */
    private int stadiumCapacity;

    // ==================================================
    // 3) 리그 / 시즌
    // ==================================================

    /** 리그 ID (EPL = 39) */
    private int leagueId;

    /** 시즌 (예: 2023) */
    private int season;

    // ==================================================
    // 생성자
    // ==================================================

    /** 기본 생성자 */
    public Team() {}

    /**
     * API → 내부 모델 변환용 생성자
     */
    public Team(int apiTeamId, String name, String country, int foundedYear,
                String logoUrl, String stadiumName, String stadiumCity, int stadiumCapacity,
                int leagueId, int season) {

        this.apiTeamId = apiTeamId;
        this.name = name;
        this.country = country;
        this.foundedYear = foundedYear;

        this.logoUrl = logoUrl;
        this.stadiumName = stadiumName;
        this.stadiumCity = stadiumCity;
        this.stadiumCapacity = stadiumCapacity;

        this.leagueId = leagueId;
        this.season = season;
    }

    // ==================================================
    // Getter / Setter
    // ==================================================

    /** 내부 DB PK */
    public long getTeamId() {
        return teamId;
    }

    /** 내부 DB PK 세팅 (DAO 전용) */
    public void setTeamId(long teamId) {
        this.teamId = teamId;
    }

    public int getApiTeamId() {
        return apiTeamId;
    }

    public void setApiTeamId(int apiTeamId) {
        this.apiTeamId = apiTeamId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public int getFoundedYear() {
        return foundedYear;
    }

    public void setFoundedYear(int foundedYear) {
        this.foundedYear = foundedYear;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getStadiumName() {
        return stadiumName;
    }

    public void setStadiumName(String stadiumName) {
        this.stadiumName = stadiumName;
    }

    public String getStadiumCity() {
        return stadiumCity;
    }

    public void setStadiumCity(String stadiumCity) {
        this.stadiumCity = stadiumCity;
    }

    public int getStadiumCapacity() {
        return stadiumCapacity;
    }

    public void setStadiumCapacity(int stadiumCapacity) {
        this.stadiumCapacity = stadiumCapacity;
    }

    public int getLeagueId() {
        return leagueId;
    }

    public void setLeagueId(int leagueId) {
        this.leagueId = leagueId;
    }

    public int getSeason() {
        return season;
    }

    public void setSeason(int season) {
        this.season = season;
    }

    // ==================================================
    // 디버깅용
    // ==================================================

    @Override
    public String toString() {
        return "Team{" +
                "teamId=" + teamId +
                ", apiTeamId=" + apiTeamId +
                ", name='" + name + '\'' +
                ", country='" + country + '\'' +
                ", foundedYear=" + foundedYear +
                ", logoUrl='" + logoUrl + '\'' +
                ", stadiumName='" + stadiumName + '\'' +
                ", stadiumCity='" + stadiumCity + '\'' +
                ", stadiumCapacity=" + stadiumCapacity +
                ", leagueId=" + leagueId +
                ", season=" + season +
                '}';
    }
}
