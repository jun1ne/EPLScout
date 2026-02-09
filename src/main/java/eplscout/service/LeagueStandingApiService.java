package eplscout.service;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * LeagueStandingApiService
 *
 * 역할
 * - 외부 API-Football Standings API 호출
 * - JSON 원본 그대로 반환
 *
 * 
 * - 여기서는 가공 
 * - Service(LeagueStandingService)에서 파싱
 */
public class LeagueStandingApiService {

    //  이미 쓰고 있는 API KEY 재사용
    private static final String API_KEY = "a170bd56a00dfe33e79d77ee714398ac";

    public JSONObject fetchStandings(int leagueId, int season) throws Exception {

        String urlStr =
                "https://v3.football.api-sports.io/standings"
              + "?league=" + leagueId
              + "&season=" + season;

        URL url = new URL(urlStr);
        HttpURLConnection conn =
                (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("GET");
        conn.setRequestProperty("x-apisports-key", API_KEY);

        BufferedReader br =
                new BufferedReader(
                        new InputStreamReader(conn.getInputStream())
                );

        StringBuilder sb = new StringBuilder();
        String line;

        while ((line = br.readLine()) != null) {
            sb.append(line);
        }

        br.close();

        return new JSONObject(sb.toString());
    }
}
