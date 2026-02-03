package controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import db.DBUtil;

import java.sql.Connection;

@RestController
public class TestController {

    @GetMapping("/api/db")
    public String testDb() {
        try (Connection conn = DBUtil.getConnection()) {
            return "DB CONNECT OK";
        } catch (Exception e) {
            e.printStackTrace();
            return "DB CONNECT FAIL";
        }
    }
}
