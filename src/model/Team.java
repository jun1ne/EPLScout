package model;

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

 
    // 1) 식별자 / 필수 정보


    /**
     * apiTeamId
     * - API-Football에서 내려주는 팀 고유 ID (외부 식별자)
     * - DB에서 UNIQUE (api_team_id, season) 중 api_team_id에 해당
     * - 중복 적재 방지/동기화 기준으로 사용
     */
    private int apiTeamId;

    /** 팀 이름 (예: Manchester City) */
    private String name;

    /** 국가 (예: England) */
    private String country;

    /**
     * 창단 연도
     * - API 값이 없을 수도 있어서 0 또는 NULL 성격으로 취급 가능
     */
    private int foundedYear;

    // ---------------------------
    // 2) UI/부가 정보 (API 응답에서 제공)
    // ---------------------------

    /** 팀 로고 URL (UI에서 이미지 표시용) */
    private String logoUrl;

    /** 홈 구장 이름 (venue.name) */
    private String stadiumName;

    /** 구장 도시 (venue.city) */
    private String stadiumCity;

    /** 구장 수용 인원 (venue.capacity) */
    private int stadiumCapacity;

    // ---------------------------
    // 3) DB 설계 필수 값 (NOT NULL 컬럼 대응)
    // ---------------------------

    /**
     * leagueId
     * - 리그 식별자 (EPL=39)
     * - 향후 리그 확장 대비
     */
    private int leagueId;

    /**
     * season
     * - 시즌 (예: 2023)
     * - 동일 팀도 시즌별로 별도 레코드 저장 가능
     */
    private int season;

    // 생성자

    /** 기본 생성자: 빈 객체 생성 후 setter로 채우는 방식도 지원 */
    public Team() {}

    /**
     * 풀 생성자
     * - API에서 team + venue + leagueId/season까지 한 번에 담아 생성 가능
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

    // ---------------------------
    // Getter / Setter
    // (캡슐화: 필드는 private, 접근은 메서드로)
    // ---------------------------

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

    /**
     * setter는 "void"로 만들고,
     * 전달받은 값을 필드에 세팅해야 한다.
     */
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

    /**
     * toString()
     * - 디버깅/로그 출력용
     * - 객체 출력 시 사람이 읽기 좋은 형태로 보여줌
     */
    @Override
    public String toString() {
        return "Team{" +
                "apiTeamId=" + apiTeamId +
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
