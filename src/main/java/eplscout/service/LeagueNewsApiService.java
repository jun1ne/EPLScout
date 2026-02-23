package eplscout.service;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class LeagueNewsApiService {

    private static final String API_KEY = "a170bd56a00dfe33e79d77ee714398ac";

    public JSONObject fetchLeagueNews(int leagueId) throws Exception {

        String urlStr =
                "https://v3.football.api-sports.io/news"
                        + "?league=" + leagueId;

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
