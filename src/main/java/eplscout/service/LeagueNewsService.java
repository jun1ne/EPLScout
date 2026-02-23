package eplscout.service;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class LeagueNewsService {

    private final LeagueNewsApiService apiService =
            new LeagueNewsApiService();

    public List<Map<String, Object>> getLeagueNews(int leagueId) {

        List<Map<String, Object>> list = new ArrayList<>();

        try {

            JSONObject json = apiService.fetchLeagueNews(leagueId);

            if (!json.has("response")) return list;

            JSONArray newsArray = json.getJSONArray("response");

            for (int i = 0; i < newsArray.length() && i < 5; i++) {

                JSONObject news = newsArray.getJSONObject(i);

                Map<String, Object> row = new HashMap<>();

                row.put("title", news.optString("title"));
                row.put("description", news.optString("description"));
                row.put("image", news.optString("image"));
                row.put("url", news.optString("link"));
                row.put("source", news.optString("source"));

                list.add(row);
            }

        } catch (Exception e) {
            System.out.println("뉴스 API 실패 → Mock fallback 사용");
        }

        return list;
    }
}
