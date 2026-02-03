package service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * LLMService
 *
 * - 추천 사유 자연어 생성
 * - 팀 분석 요약
 * - 규칙 기반 결과를 설명으로 변환
 *
 * 설계 원칙:
 *  - LLM은 판단 안함
 *  - 설명/요약만 담당
 */
public class LLMService {

    private static final String API_URL =
            "https://api.openai.com/v1/chat/completions";

    private static final String MODEL = "gpt-4o-mini";

    private final HttpClient client = HttpClient.newHttpClient();
    private final String apiKey = System.getenv("OPENAI_API_KEY");

    /**
     * 추천 선수 설명 생성
     */
    public String generateRecommendationReason(
            String teamName,
            String playerName,
            String position,
            double score,
            double potentialScore,
            String ruleReason
    ) {

        String prompt = """
            You are a football scouting assistant.

            The system has already selected a player using rule-based analysis.
            Your job is to explain the recommendation in natural language.

            Team: %s
            Player: %s
            Position: %s
            Immediate Performance Score: %.2f
            Potential Score: %.2f
            Rule-based Reason: %s

            Explain in 2~3 sentences why this player is a good recommendation.
            Do NOT suggest new players.
            Do NOT override the system decision.
            """.formatted(
                teamName,
                playerName,
                position,
                score,
                potentialScore,
                ruleReason
        );

        return callLLM(prompt);
    }

    /**
     * 팀 상황 요약 생성
     */
    public String summarizeTeamStatus(
            String teamName,
            String weakPosition,
            double avgAge,
            double avgRating
    ) {

        String prompt = """
            You are a football analyst.

            Summarize the current situation of the following team:

            Team: %s
            Weak Position: %s
            Average Age: %.1f
            Average Rating: %.2f

            Write a concise 2-sentence summary.
            """.formatted(
                teamName,
                weakPosition,
                avgAge,
                avgRating
        );

        return callLLM(prompt);
    }

    /**
     * OpenAI API 호출 공통 메서드
     */
    private String callLLM(String prompt) {

        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException(
                "OPENAI_API_KEY 환경변수가 설정되지 않았습니다."
            );
        }

        String requestBody = """
            {
              "model": "%s",
              "messages": [
                {
                  "role": "system",
                  "content": "You are a helpful assistant."
                },
                {
                  "role": "user",
                  "content": "%s"
                }
              ],
              "temperature": 0.5
            }
            """.formatted(MODEL, escapeJson(prompt));

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            return extractMessage(response.body());

        } catch (Exception e) {
            throw new RuntimeException("LLM API 호출 실패", e);
        }
    }

    /**
     * 응답 JSON에서 content 추출 (간단 파싱)
     */
    private String extractMessage(String json) {

        int start = json.indexOf("\"content\":\"");
        if (start == -1) return "LLM 응답 파싱 실패";

        start += 11;
        int end = json.indexOf("\"", start);

        return json.substring(start, end)
                   .replace("\\n", "\n")
                   .replace("\\\"", "\"");
    }

    /**
     * JSON escape 처리
     */
    private String escapeJson(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }
}
