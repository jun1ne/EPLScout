package db;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * DBUtil
 * - DB 연결(Connection) 만드는 역할만 담당
 * - SQL 실행 로직(DAO)랑 분리해서 코드가 깔끔해짐
 *
 * (나중에 DB 계정/URL 바뀌면 여기만 고치면 됨)
 */
public class DBUtil {

    /**
     * DB 이름: epl_scout_db
     * - serverTimezone은 시간 관련 경고/오류 방지용
     * - characterEncoding은 한글 깨짐 방지용
     */
    private static final String URL =
    "jdbc:mariadb://localhost:3306/epl_scout_db?serverTimezone=Asia/Seoul&characterEncoding=utf8";


    // DB접속 계정, 비번
    private static final String USER = "epl_app";
    private static final String PASSWORD = "Epl!2345";

    public static Connection getConnection() throws Exception {
        // DriverManager가 URL/USER/PW로 DB 연결을 만들어줌
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
