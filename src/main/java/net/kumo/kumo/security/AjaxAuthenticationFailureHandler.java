package net.kumo.kumo.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.kumo.kumo.domain.entity.LoginHistoryEntity; // ★ 엔티티 임포트
import net.kumo.kumo.domain.entity.UserEntity;
import net.kumo.kumo.repository.LoginHistoryRepository; // ★ 레포지토리 임포트
import net.kumo.kumo.repository.UserRepository;
import org.springframework.context.MessageSource;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.LocaleResolver;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AjaxAuthenticationFailureHandler implements AuthenticationFailureHandler {
	
	private final ObjectMapper objectMapper;
	private final UserRepository userRepository;
	private final MessageSource messageSource;
	private final LocaleResolver localeResolver;
	
	// ★ DB 저장을 위해 추가된 레포지토리
	private final LoginHistoryRepository loginHistoryRepository;
	
	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
			throws IOException, ServletException {
		
		// 1. 기본 정보 수집
		String email = request.getParameter("email");
		String clientIp = getClientIp(request); // IP 주소
		String userAgent = request.getHeader("User-Agent"); // ★ 브라우저/기기 정보 (봇 탐지용)
		Locale locale = localeResolver.resolveLocale(request);
		
		String errorMessage = "";
		int failCount = 0;
		boolean showCaptcha = false;
		
		// ★ DB에 저장할 실패 사유를 담을 변수
		String failReasonLog = "";
		
		if (email != null) {
			// [추가] 리캡차 검증 실패 케이스 처리
			if ("CAPTCHA_FAILED".equals(exception.getMessage())) {
				showCaptcha = true;
				failReasonLog = "리캡차 인증 실패";
				errorMessage = messageSource.getMessage("login.fail.captcha", null, locale);
				log.warn("[보안 경고] 리캡차 미인증 또는 검증 실패 | Email: {}", email);
			} else {
				Optional<UserEntity> userOptional = userRepository.findByEmail(email);
				
				if (userOptional.isPresent()) {
					// [CASE A] 존재하는 아이디 -> 비밀번호 틀림
					UserEntity user = userOptional.get();
					user.increaseFailCount();
					userRepository.save(user);
					
					failCount = user.getLoginFailCount();
					
					// 2. 실패 사유 구체화 (DB 저장용)
					if (failCount >= 5) {
						showCaptcha = true;
						failReasonLog = "비밀번호 5회 이상 오류 (캡차 요구)";
						errorMessage = messageSource.getMessage("login.fail.captcha", null, locale);
						
						// [콘솔 로그] 강력 경고
						log.warn("[보안 경고] 5회 이상 실패 계정 감지 | IP: {} | Email: {}", clientIp, email);
					} else {
						failReasonLog = "비밀번호 불일치 (" + failCount + "회)";
						errorMessage = messageSource.getMessage("login.fail.mismatch", null, locale);
						
						// [콘솔 로그] 일반 경고
						log.warn("[로그인 실패] 비밀번호 불일치 | IP: {} | Email: {} | 횟수: {}", clientIp, email, failCount);
					}
					
				} else {
					// [CASE B] 없는 아이디
					failReasonLog = "존재하지 않는 계정 시도";
					
					// [콘솔 로그]
					log.warn("[로그인 실패] 존재하지 않는 계정 | IP: {} | 입력한 Email: {}", clientIp, email);
					
					errorMessage = messageSource.getMessage("login.fail.mismatch", null, locale);
				}
			}
		} else {
			// 이메일 파라미터 누락
			failReasonLog = "이메일 파라미터 누락 (비정상 요청)";
			log.warn("[로그인 실패] 파라미터 없음 | IP: {}", clientIp);
			
			errorMessage = messageSource.getMessage("login.fail.mismatch", null, locale);
		}
		
		// 3. ★★★ [핵심] 로그인 실패 이력 DB 저장 (어드민용) ★★★
		try {
			LoginHistoryEntity history = LoginHistoryEntity.builder()
					.email(email == null ? "unknown" : email) // 이메일 없으면 unknown 처리
					.clientIp(clientIp)
					.userAgent(userAgent) // 어떤 브라우저로 접속했는지
					.isSuccess(false)     // 실패함
					.failReason(failReasonLog) // 위에서 정한 구체적 사유
					.build();
			
			loginHistoryRepository.save(history);
		} catch (Exception e) {
			// 로그 저장이 실패했다고 해서 로그인 로직 자체가 멈추면 안 되므로 예외 처리
			log.error("로그인 이력 저장 중 오류 발생", e);
		}
		
		// 4. 클라이언트(프론트엔드) 응답 전송
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType("application/json;charset=UTF-8");
		
		Map<String, Object> data = new HashMap<>();
		data.put("code", "LOGIN_FAILED");
		data.put("message", errorMessage);
		data.put("failCount", failCount);
		data.put("showCaptcha", showCaptcha);
		
		objectMapper.writeValue(response.getWriter(), data);
	}
	
	// 클라이언트의 진짜 IP를 가져오는 메서드
	private String getClientIp(HttpServletRequest request) {
		String ip = request.getHeader("X-Forwarded-For");
		
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("Proxy-Client-IP");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("WL-Proxy-Client-IP");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("HTTP_CLIENT_IP");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("HTTP_X_FORWARDED_FOR");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getRemoteAddr();
		}
		
		return ip;
	}
}