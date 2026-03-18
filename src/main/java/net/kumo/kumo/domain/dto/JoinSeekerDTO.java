package net.kumo.kumo.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

/**
 * 구직자(Seeker) 회원가입 시 클라이언트 폼으로부터
 * 개인 신상 정보 및 계정 데이터를 수신하는 DTO 클래스입니다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JoinSeekerDTO {

    private String nameKanjiSei;
    private String nameKanjiMei;
    private String nameKanaSei;
    private String nameKanaMei;

    private String nickname;
    private String email;
    private String password;

    private Integer birthYear;
    private Integer birthMonth;
    private Integer birthDay;

    private String gender;
    private String contact;

    private String zipCode;
    private String addressMain;
    private String addressDetail;

    private String addrPrefecture;
    private String addrCity;
    private String addrTown;

    private Double latitude;
    private Double longitude;

    private String joinPath;
    private boolean adReceive;

    /**
     * 분리된 년/월/일 데이터를 결합하여 LocalDate 객체로 반환합니다.
     *
     * @return 결합된 생년월일 LocalDate 객체 (유효하지 않을 경우 null)
     */
    public LocalDate getBirthDate() {
        if (birthYear == null || birthMonth == null || birthDay == null) {
            return null;
        }
        try {
            return LocalDate.of(birthYear, birthMonth, birthDay);
        } catch (Exception e) {
            return null;
        }
    }
}