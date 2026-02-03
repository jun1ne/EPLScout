package db;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnectionTest {

    public static void main(String[] args) {

        String url = "jdbc:mariadb://localhost:3306/epl_scout_db";
        String user = "root";
        String password = "0000"; // ← 여기 네 비번

        try {
            Connection conn = DriverManager.getConnection(url, user, password);
            System.out.println("✅ 자바 → DB 연결 성공");
            conn.close();
        } catch (Exception e) {
            System.out.println("❌ 자바 → DB 연결 실패");
            e.printStackTrace();
        }
    }
}
