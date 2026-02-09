package eplscout.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

/**
 * LLMService
 *
 * 역할:
 * - 추천 결과에 대한 "설명"만 생성
 * - 팀/선수에 대한 판단은 절대 하지 않음
 */
@Service   //  핵심
public class LLMService {

    private static final String API_URL =
            "https://api.openai.com/v1/chat/completions";

    private static final String MODEL = "gpt-4o-mini";

    private final HttpClient client = HttpClient.newHttpClient();
    private final String apiKey = System.getenv("OPENAI_API_KEY");

    /* =====================================================
       1. 추천 선수 설명 생성
       ===================================================== */
    public String generateRecommendationReason(
            String teamName,
            String playerName,
            String position,
            double score,
            double potentialScore,
            String ruleReason
    ) {

        String prompt = """
당신은 축구 스카우트 분석가입니다.

이 시스템은 이미 규칙 기반 분석을 통해
선수 추천과 포지션 우선순위 판단을 완료했습니다.

당신의 역할은
"왜 이 선수가 추천되었는지"를
사람이 이해하기 쉬운 설명으로 풀어주는 것입니다.

팀 이름: %s
선수 이름: %s
포지션: %s
즉시 전력 점수: %.2f
포텐셜 점수: %.2f
시스템 추천 사유: %s

조건:
- 추천 이유만 설명
- 새로운 선수 제안 금지
- 2~3문장
- 반드시 한국어
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

    /* =====================================================
       2. 간단 호출용
       ===================================================== */
    public String generateRecommendationReason(
            String teamName,
            String playerName,
            String position,
            double score
    ) {
        return generateRecommendationReason(
                teamName,
                playerName,
                position,
                score,
                0.0,
                "시스템 점수 기반 추천"
        );
    }

    /* =====================================================
       3. OpenAI API 호출
       ===================================================== */
    private String callLLM(String prompt) {

        if (apiKey == null || apiKey.isEmpty()) {
            return "LLM API 키가 설정되지 않았습니다.";
        }

        String requestBody = """
        {
          "model": "%s",
          "messages": [
            {
              "role": "system",
              "content": "당신은 축구 스카우트 전문가입니다. 모든 응답은 한국어로 작성하세요."
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
            return "추천 사유 생성에 실패했습니다.";
        }
    }

    /* =====================================================
       4. 응답 파싱
       ===================================================== */
    private String extractMessage(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);

            return root
                    .path("choices")
                    .get(0)
                    .path("message")
                    .path("content")
                    .asText();

        } catch (Exception e) {
            return "추천 사유 생성에 실패했습니다.";
        }
    }

    /* =====================================================
       5. JSON escape
       ===================================================== */
    private String escapeJson(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }


            /* =====================================================
        6. 선수 플레이 스타일 설명 (스탯 기반)
        ===================================================== */
        public String generatePlayerStyleDescription(
                String playerName,
                String position,
                String statSummary
        ) {

            String prompt = """
        당신은 축구 스카우트 보고서를 작성하는 전문가입니다.

        아래는 한 선수의 시즌 스탯을 분석해
        시스템이 요약한 "플레이 특징"입니다.

        선수 이름: %s
        포지션: %s
        플레이 특징 요약:
        %s

        요구사항:
        - 이 선수가 어떤 플레이 스타일의 선수인지 설명
        - 전술적 역할, 성향 중심
        - 수치 언급 금지
        - 2~3문장
        - 반드시 한국어
        """.formatted(
                    playerName,
                    position,
                    statSummary
            );

            return callLLM(prompt);
        }

}
