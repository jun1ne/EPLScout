package eplscout.service;

import eplscout.model.Team;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * TeamApiService
 * - "외부 API 통신" 전담 클래스
 * - DB/GUI/추천 로직은 모름(관심 없음)
 * - 오직: API에서 팀 목록을 받아 → Team 객체 리스트로 변환해서 반환
 *
 * - 외부 의존성(API 호출)을 Service로 분리해서, DB 저장/추천 로직과 독립적으로 유지 가능
 * - API가 바뀌어도(도메인/헤더/JSON 구조 변경) 이 클래스만 수정하면 전체 시스템 영향 최소화
 */
public class TeamApiService {


    private static final String API_KEY = "a170bd56a00dfe33e79d77ee714398ac";

    /**
     * 고정해서 쓰는 값들
     * - DB 저장할 때도 같이 들어가야 해서 상수로 빼둠
     * - leagueId=39(EPL), season=2023
     *
     * (나중에 UI에서 시즌/리그 선택 기능 만들면 이 값들은 파라미터로 바뀜)
     */
    private static final int LEAGUE_ID = 39;
    private static final int SEASON = 2023;

    /**
     * 공식 대시보드(API-Sports) 기반 API 엔드포인트
     *
     * query param:
     * - league=39 : EPL
     * - season=2023 : 2023 시즌(완료된 시즌이라 데이터가 안정적)
     */
    private static final String API_URL =
            "https://v3.football.api-sports.io/teams?league=39&season=2023";

    /**
     * EPL 팀 목록 가져오기
     * @return List<Team> (API 데이터를 시스템의 Team 객체로 변환한 결과)
     */
    public List<Team> getEplTeams() throws Exception {

        // Team 객체들을 담아서 호출자(App/Controller)에게 넘겨줄 리스트
        List<Team> teamList = new ArrayList<>();

        /**
         * 1) HTTP 요청 생성
         */
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("x-apisports-key", API_KEY)  //  공식 방식 헤더
                .GET()
                .build();

        /**
         * 2) 요청 전송(서버 호출)
         *
         * - HttpClient: Java 표준 HTTP 통신 도구
         * - response.body(): 서버가 준 JSON 문자열
         */
        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        /**
         * 3) JSON 파싱 시작
         *
         * response.body()는 "문자열"이므로
         * JSON 라이브러리(org.json)를 이용해서 JSONObject로 변환
         */
        JSONObject json = new JSONObject(response.body());

        /**
         * 4) 방어 코드
         *
         * API는 항상 실패할 수 있다.
         * 예) 키 오류, 한도 초과, 서버 에러, 파라미터 오류
         *
         * 그럴 때 정상 응답 구조("response": [...])가 없을 수 있으니,
         * 존재 여부를 먼저 확인해서 프로그램이 터지지 않게 한다.
         * - 외부 API 실패를 고려해 예외 상황을 처리
         */
        if (!json.has("response")) {
            System.out.println("API Error Response (response 키 없음):");
            System.out.println(json.toString(2));
            return teamList; // 빈 리스트 반환(호출 측에서 처리 가능)
        }

        /**
         * 5) 실제 데이터 추출
         *
         * API-Football teams 응답 구조:
         * {
         *   "results": 20,
         *   "response": [
         *      { "team": {...}, "venue": {...} },
         *      ...
         *   ]
         * }
         *
         * 우리가 필요한 팀 정보는:
         * response[i].team, response[i].venue 안에 있다.
         * (DB 테이블에 logo_url, stadium_*도 있으니까 여기서 같이 채워줌)
         */
        JSONArray teams = json.getJSONArray("response");

        /**
         * 6) response 배열을 순회하며 Team 객체로 변환
         * - JSON → Team(우리 시스템 객체)로 변환하면,
         *   이후 DB 저장/추천/화면 출력에서 재사용하기 쉬워진다.
         */
        for (int i = 0; i < teams.length(); i++) {

            // response[i] (team + venue가 함께 들어있는 객체)
            JSONObject item = teams.getJSONObject(i);

            // item 안의 "team" 객체
            JSONObject teamJson = item.getJSONObject("team");

            // item 안의 "venue" 객체 (구장 정보)
            // 어떤 경우 venue 데이터가 비어있을 수도 있어서 opt로 안전하게 처리
            JSONObject venueJson = item.optJSONObject("venue");

            // ---------------------------
            // team 정보 추출
            // ---------------------------
            int apiTeamId = teamJson.getInt("id");
            String name = teamJson.getString("name");
            String country = teamJson.getString("country");

            // founded는 없을 수 있으니 optInt로 안전하게 처리
            int founded = teamJson.optInt("founded", 0);

            // 로고 URL (UI에서 이미지로 쓰기 좋음)
            String logoUrl = teamJson.optString("logo", null);

            // ---------------------------
            // venue 정보 추출 (null 대비)
            // ---------------------------
            String stadiumName = null;
            String stadiumCity = null;
            int stadiumCapacity = 0;

            if (venueJson != null) {
                stadiumName = venueJson.optString("name", null);
                stadiumCity = venueJson.optString("city", null);
                stadiumCapacity = venueJson.optInt("capacity", 0);
            }

            // ---------------------------
            // DB 필수값(league_id, season) 세팅
            // - team 테이블에서 NOT NULL이라 무조건 넣어야 저장 가능
            // ---------------------------
            int leagueId = LEAGUE_ID;
            int season = SEASON;

            // Team 객체 생성(풀 생성자 사용) 후 리스트에 추가
            Team team = new Team(
                    apiTeamId, name, country, founded,
                    logoUrl, stadiumName, stadiumCity, stadiumCapacity,
                    leagueId, season
            );

            teamList.add(team);
        }

        // 호출자에게 팀 목록 반환
        return teamList;
    }
}
