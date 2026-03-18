package net.kumo.kumo.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 아이디(이메일) 찾기 기능을 수행하기 위해 클라이언트로부터
 * 사용자의 식별 정보를 전달받는 DTO 클래스입니다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FindIdDTO {

    /** 사용자의 전체 이름 (성+이름) */
    private String name;

    /** 사용자의 연락처 (전화번호) */
    private String contact;

    /** 사용자 권한 유형 ("SEEKER" 또는 "RECRUITER") */
    private String role;

}