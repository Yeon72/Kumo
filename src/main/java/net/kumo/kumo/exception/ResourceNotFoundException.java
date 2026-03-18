package net.kumo.kumo.exception;

/**
 * 클라이언트가 요청한 데이터나 리소스를 데이터베이스 등에서
 * 찾을 수 없을 때 발생하는 커스텀 예외 클래스입니다. (HTTP 404 연동)
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}