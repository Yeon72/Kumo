package net.kumo.kumo.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 비밀번호 찾기(초기화) 기능을 수행하기 위해 클라이언트로부터
 * 사용자의 식별 정보 및 이메일을 전달받는 DTO 클래스입니다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FindPwDTO {

    /** 사용자의 전체 이름 (성+이름) */
    private String name;

    /** 사용자의 연락처 (전화번호) */
    private String contact;

    /** 사용자 권한 유형 ("SEEKER" 또는 "RECRUITER") */
    private String role;

    /** 가입 시 등록한 사용자 이메일 (계정 ID) */
    private String email;

}