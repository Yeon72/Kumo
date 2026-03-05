package net.kumo.kumo.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import net.kumo.kumo.domain.dto.ErrorResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.io.PrintWriter;
import java.io.StringWriter;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {
	// 🌟 [추가] 에러 객체에서 StackTrace(로그)를 문자열로 뽑아내는 헬퍼 메서드
	private String getStackTrace(Exception e) {
		if (e == null) return "";
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		return sw.toString();
	}
	
	
	// ==========================================
	// 🌟 [핵심] API 요청인지 브라우저 요청인지 구분해서 응답을 만들어주는 마법의 메서드
	// ==========================================
	private Object makeResponse(HttpServletRequest request, HttpStatus status, String message, Exception e) {
		String acceptHeader = request.getHeader("Accept");
		String uri = request.getRequestURI();
		
		// 1. AJAX 요청이거나 URL에 /api/가 포함된 경우 -> 기존처럼 JSON 리턴!
		if ((acceptHeader != null && acceptHeader.contains("application/json")) ||
				(uri != null && uri.contains("/api/"))) {
			
			ErrorResponseDTO response = ErrorResponseDTO.builder()
					.status(status.value())
					.error(status.getReasonPhrase())
					.message(message)
					.build();
			return ResponseEntity.status(status).body(response);
		}
		// 2. 일반 브라우저 화면 요청인 경우 -> 에러 HTML 페이지 리턴!
		else {
			ModelAndView mav = new ModelAndView("errorView/errorPage"); // error_page.html을 띄웁니다
			mav.addObject("errorCode", status.value());
			mav.addObject("errorMessage", message);
			// 🌟 에러가 존재하면 로그 텍스트를 화면으로 넘김
			if (e != null) {
				mav.addObject("errorTrace", getStackTrace(e));
			}
			return mav;
		}
	}
	
	// ==========================================
	// 1. 401 Unauthorized
	// ==========================================
	@ExceptionHandler(UnauthorizedException.class)
	public Object handleUnauthorizedException(UnauthorizedException e, HttpServletRequest request) {
		log.warn("🚨 [401 Unauthorized] {}", e.getMessage());
		return makeResponse(request, HttpStatus.UNAUTHORIZED, e.getMessage(), e);
	}
	
	// ==========================================
	// 2. 404 Not Found (존재하지 않는 데이터)
	// ==========================================
	@ExceptionHandler(ResourceNotFoundException.class)
	public Object handleResourceNotFoundException(ResourceNotFoundException e, HttpServletRequest request) {
		log.warn("🚨 [404 Not Found] {}", e.getMessage());
		return makeResponse(request, HttpStatus.NOT_FOUND, e.getMessage(), e);
	}
	
	// ==========================================
	// 3. 404 Not Found (잘못된 URL 요청 - Spring 기본 예외)
	// ==========================================
	@ExceptionHandler(NoHandlerFoundException.class)
	public Object handleNoHandlerFoundException(NoHandlerFoundException e, HttpServletRequest request) {
		log.warn("🚨 [404 잘못된 URL 요청] {}", e.getRequestURL());
		return makeResponse(request, HttpStatus.NOT_FOUND, "요청하신 페이지를 찾을 수 없습니다.", e);
	}
	
	// ==========================================
	// 4. 500 Internal Server Error (최후의 보루)
	// ==========================================
	@ExceptionHandler(Exception.class)
	public Object handleAllUncaughtException(Exception e, HttpServletRequest request) {
		log.error("🔥 [500 Internal Server Error] 예상치 못한 서버 에러 발생!", e);
		
		// 📌 나중에 여기에 어드민 로그(DB) 저장 로직을 한 줄 쓱 추가하면 목표 1번도 바로 달성됩니다!
		// adminLogService.saveErrorLog(e);
		
		return makeResponse(request, HttpStatus.INTERNAL_SERVER_ERROR, "서버에 일시적인 문제가 발생했습니다. 잠시 후 다시 시도해주세요.", e);
	}
}