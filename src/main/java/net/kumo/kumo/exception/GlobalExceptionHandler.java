package net.kumo.kumo.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.kumo.kumo.domain.dto.ErrorResponseDTO;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * 애플리케이션 전역에서 발생하는 예외(Exception)를 감지하고 처리하는 공통 핸들러 클래스입니다.
 * 클라이언트의 요청 타입(API 비동기 통신 vs 일반 브라우저 렌더링)을 구분하여
 * 적절한 형태(JSON DTO 또는 에러 페이지 뷰)로 응답을 반환합니다.
 */
@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    /**
     * 예외 객체로부터 상세한 StackTrace 문자열을 추출합니다.
     *
     * @param e 발생한 예외 객체
     * @return 추출된 StackTrace 문자열
     */
    private String getStackTrace(Exception e) {
        if (e == null) return "";
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    /**
     * 클라이언트의 요청 정보(Header, URI)를 분석하여 예외 응답의 포맷을 결정합니다.
     * REST API 요청일 경우 JSON 형태의 ResponseEntity를, 일반 웹 요청일 경우 ModelAndView를 반환합니다.
     *
     * @param request HTTP 요청 객체
     * @param status  HTTP 상태 코드
     * @param message 다국어 처리가 완료된 에러 메시지
     * @param e       발생한 예외 객체
     * @return ResponseEntity (API 요청 시) 또는 ModelAndView (웹 요청 시)
     */
    private Object makeResponse(HttpServletRequest request, HttpStatus status, String message, Exception e) {
        String acceptHeader = request.getHeader("Accept");
        String uri = request.getRequestURI();

        if ((acceptHeader != null && acceptHeader.contains("application/json")) ||
                (uri != null && uri.contains("/api/"))) {

            ErrorResponseDTO response = ErrorResponseDTO.builder()
                    .status(status.value())
                    .error(status.getReasonPhrase())
                    .message(message)
                    .build();
            return ResponseEntity.status(status).body(response);
        } else {
            ModelAndView mav = new ModelAndView("errorView/errorPage");
            mav.addObject("errorCode", status.value());
            mav.addObject("errorMessage", message);

            if (e != null) {
                mav.addObject("errorTrace", getStackTrace(e));
            }
            return mav;
        }
    }

    /**
     * 인증되지 않은 사용자의 접근 등 권한 관련 예외(401)를 처리합니다.
     */
    @ExceptionHandler(UnauthorizedException.class)
    public Object handleUnauthorizedException(UnauthorizedException e, HttpServletRequest request) {
        log.warn("[401 Unauthorized] {}", e.getMessage());
        String message = messageSource.getMessage("error.unauthorized", null, e.getMessage(), LocaleContextHolder.getLocale());
        return makeResponse(request, HttpStatus.UNAUTHORIZED, message, e);
    }

    /**
     * 요청한 리소스(데이터)를 찾을 수 없는 예외(404)를 처리합니다.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public Object handleResourceNotFoundException(ResourceNotFoundException e, HttpServletRequest request) {
        log.warn("[404 Not Found] {}", e.getMessage());
        String message = messageSource.getMessage("error.resource_not_found", null, e.getMessage(), LocaleContextHolder.getLocale());
        return makeResponse(request, HttpStatus.NOT_FOUND, message, e);
    }

    /**
     * 존재하지 않는 URL 경로로 접근했을 때 발생하는 Spring 기본 예외(404)를 처리합니다.
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public Object handleNoHandlerFoundException(NoHandlerFoundException e, HttpServletRequest request) {
        log.warn("[404 Bad Request URL] {}", e.getRequestURL());
        String message = messageSource.getMessage("error.page_not_found", null, "요청하신 페이지를 찾을 수 없습니다.", LocaleContextHolder.getLocale());
        return makeResponse(request, HttpStatus.NOT_FOUND, message, e);
    }

    /**
     * 명시적으로 핸들링되지 않은 모든 예외(500)를 최종적으로 처리합니다.
     */
    @ExceptionHandler(Exception.class)
    public Object handleAllUncaughtException(Exception e, HttpServletRequest request) {
        log.error("[500 Internal Server Error] 예상치 못한 서버 에러 발생", e);
        String message = messageSource.getMessage("error.internal_server", null, "서버에 일시적인 문제가 발생했습니다. 잠시 후 다시 시도해주세요.", LocaleContextHolder.getLocale());
        return makeResponse(request, HttpStatus.INTERNAL_SERVER_ERROR, message, e);
    }
}