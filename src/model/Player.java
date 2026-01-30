package model;

import java.time.LocalDate;

/**
 * Player (Model / Entity)
 * - 선수의 "기본 정보"만 담는 객체
 * - 경기/시즌 스탯은 별도 테이블에서 관리
 *
 * - players API 기준 필드 + birthDate 추가
 */
public class Player {

    // API-Football 선수 고유 ID (외부 식별자)
    private int apiPlayerId;

    // 선수 이름
    private String name;

    // API 기준 나이 (표시/필터용)
    private int age;

    // ★ 생년월일 (계산 기준값)
    private LocalDate birthDate;

    // 등번호 (스쿼드 기준)
    private int number;

    // 포지션
    private String position;

    // 선수 사진 URL
    private String photoUrl;

    public Player() {}

    public Player(int apiPlayerId, String name, int age, LocalDate birthDate,
                  int number, String position, String photoUrl) {
        this.apiPlayerId = apiPlayerId;
        this.name = name;
        this.age = age;
        this.birthDate = birthDate;
        this.number = number;
        this.position = position;
        this.photoUrl = photoUrl;
    }

    public int getApiPlayerId() {
        return apiPlayerId;
    }

    public void setApiPlayerId(int apiPlayerId) {
        this.apiPlayerId = apiPlayerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    @Override
    public String toString() {
        return "Player{" +
                "apiPlayerId=" + apiPlayerId +
                ", name='" + name + '\'' +
                ", age=" + age +
                ", birthDate=" + birthDate +
                ", number=" + number +
                ", position='" + position + '\'' +
                '}';
    }
}
