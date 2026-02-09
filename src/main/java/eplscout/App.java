package eplscout;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * App (Spring Boot 실행 진입점)
 *
 * 역할:
 * - Spring Boot 서버 실행
 * - Controller / Service / DAO 자동 스캔
 *
 * ※ 배치/콘솔 테스트 로직은
 *   AppLLMTest 또는 별도 테스트 클래스로 분리
 */
@SpringBootApplication
public class App {

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}
