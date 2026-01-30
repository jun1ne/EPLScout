import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * AppLLMTest
 *
 * - OpenAI API 단독 호출 테스트용 실행 클래스
 * - 추천 로직 / DB / 파싱 전혀 없음
 * - "LLM이 실제로 응답하는지"만 확인
 */
public class AppLLMTest {

    private static final String API_URL =
            "https://api.openai.com/v1/chat/completions";

    private final String apiKey;

    public AppLLMTest() {
        this.apiKey = System.getenv("OPENAI_API_KEY");

        if (this.apiKey == null || this.apiKey.isEmpty()) {
            throw new RuntimeException(
                "OPENAI_API_KEY 환경변수가 설정되지 않았습니다."
            );
        }
    }

    /**
     * 테스트용 LLM 호출
     */
    public String simpleTest() {

        String requestBody = """
        {
          "model": "gpt-4o-mini",
          "messages": [
            {
              "role": "user",
              "content": ""content": "다음 질문에 반드시 한국어로만 답해줘. 부카요 사카가 왜 좋은 축구 선수인지 한 문장으로 설명해줘."
"
            }
          ],
          "temperature": 0.7
        }
        """;

        try {
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            return response.body();

        } catch (Exception e) {
            throw new RuntimeException("LLM API 호출 실패", e);
        }
    }

    /**
     *  JVM 실행 진입점
     */
    public static void main(String[] args) {

        System.out.println("=================================");
        System.out.println("LLM API 단독 테스트 시작");
        System.out.println("=================================");

        AppLLMTest test = new AppLLMTest();
        String result = test.simpleTest();

        System.out.println("LLM 응답 원문:");
        System.out.println(result);

        System.out.println("=================================");
        System.out.println("LLM API 단독 테스트 종료");
        System.out.println("=================================");
    }
}
