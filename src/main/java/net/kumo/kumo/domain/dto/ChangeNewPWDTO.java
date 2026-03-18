package net.kumo.kumo.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 비밀번호 재설정 시 사용자의 이메일과 새로운 비밀번호 데이터를
 * 클라이언트로부터 전달받기 위한 DTO 클래스입니다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangeNewPWDTO {

    private String email;
    private String password;

}