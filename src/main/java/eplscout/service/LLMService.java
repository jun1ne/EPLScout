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

    /* =====================================================
       7. 고급 추천 설명 (스탯 기반 + 포지션 분기)
       ===================================================== */
    public String generateAdvancedRecommendationReason(
            String teamName,
            String playerName,
            String position,
            int goals,
            int assists,
            double rating,
            int appearances,
            double score,
            double potentialScore
    ) {

        String positionGuide = switch (position) {

            case "FW" -> """
- 득점력과 결정력 중심으로 해석
- 공격 생산성, 침투 움직임 언급
""";

            case "MF" -> """
- 경기 조율 능력과 패스 영향력 중심 해석
- 창의성, 공격 연결고리 역할 강조
""";

            case "DF" -> """
- 수비 안정성, 경기 운영 기여도 중심 해석
- 빌드업 기여 및 수비 리더십 언급
""";

            case "GK" -> """
- 실점 억제 능력, 선방 영향력 중심 해석
- 수비 조직 안정감 기여 강조
""";

            default -> """
- 전반적인 경기 영향력 중심 분석
""";
        };

        String prompt = """
당신은 유럽 빅클럽 수석 스카우트입니다.

이 선수는 이미 수치 기반 시스템 평가를 통해 추천되었습니다.
당신의 역할은 계산된 수치를 기반으로 전문 리포트를 작성하는 것입니다.

팀: %s
선수: %s
포지션: %s

[시즌 성과]
출전 경기 수: %d
득점: %d
어시스트: %d
평균 평점: %.2f

[시스템 평가]
즉시 전력 점수: %.2f
포텐셜 점수: %.2f

[포지션 분석 가이드]
%s

요구사항:
1. 포지션 특성에 맞게 분석
2. 평균 평점 기반 경기 영향력 설명
3. 점수 해석 포함
4. 성장 가능성 또는 향후 가치 언급
5. 반드시 수치를 포함
6. 4~5문장
7. 한국어만 사용
""".formatted(
                teamName,
                playerName,
                position,
                appearances,
                goals,
                assists,
                rating,
                score,
                potentialScore,
                positionGuide
        );

        return callLLM(prompt);
    }

}
