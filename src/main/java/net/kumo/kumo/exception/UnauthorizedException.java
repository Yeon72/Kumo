package net.kumo.kumo.exception;

/**
 * 인증 정보가 누락되었거나, 권한이 없는 리소스에 접근을 시도할 때
 * 발생하는 커스텀 예외 클래스입니다. (HTTP 401 연동)
 */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}