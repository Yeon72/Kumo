package net.kumo.kumo.domain.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 프론트엔드로 반환되는 전역 예외 처리(Global Exception) 및
 * 공통 에러 응답(Error Response) 데이터를 담는 DTO 클래스입니다.
 */
@Getter
@Builder
public class ErrorResponseDTO {

    /** 에러 발생 시간 (기본값: 현재 시간) */
    @Builder.Default
    private final LocalDateTime timestamp = LocalDateTime.now();

    /** HTTP 상태 코드 (예: 400, 401, 403, 500 등) */
    private final int status;

    /** 에러 유형 명칭 (예: "Unauthorized", "Bad Request") */
    private final String error;

    /** 클라이언트에게 노출될 상세 에러 메시지 */
    private final String message;

}