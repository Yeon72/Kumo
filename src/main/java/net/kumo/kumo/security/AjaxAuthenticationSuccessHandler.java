package net.kumo.kumo.security;


import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import net.kumo.kumo.domain.entity.LoginHistoryEntity;
import net.kumo.kumo.domain.entity.UserEntity;
import net.kumo.kumo.repository.LoginHistoryRepository;
import net.kumo.kumo.repository.UserRepository;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.codec.Utf8;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.LocaleResolver;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AjaxAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
	
	private final UserRepository userRepository;
	private final LoginHistoryRepository loginHistoryRepository;
	private final ObjectMapper objectMapper; // ★ JSON 변환기 (Spring Boot에 기본 내장됨)
	
	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,  Authentication authentication)
			throws IOException, ServletException {
		
		// 실패 횟수 초기화
		String email = request.getParameter("email");
		String clientIp = getClientIp(request);
		String userAgent = request.getHeader("User-Agent"); // ★ 브라우저/기기 정보 (봇 탐지용)
		String saveId = request.getParameter("saveId");

		Integer resetLoginFailCount = 0;
		String Encode = "UTF-8";
		
		// 실패횟수 초기화
		Optional<UserEntity> UserOptional = userRepository.findByEmail(email);
		UserEntity entity = UserOptional.get();
		entity.setLoginFailCount(resetLoginFailCount);
		userRepository.save(entity);
		
		// 로그 기록
		LoginHistoryEntity loginHistoryEntity = LoginHistoryEntity.builder().
				email(email).
				clientIp(clientIp).
				userAgent(userAgent).
				isSuccess(true).
				failReason(null).
				build();
		loginHistoryRepository.save(loginHistoryEntity);
		
		
		// 체크박스 쿠키 생성 및 설정하기
		Cookie cookie = new Cookie("saveEmail",email);
		if(saveId != null && saveId.equals("on")){
			cookie.setMaxAge(60*60*24*30);
		}else {
			cookie.setMaxAge(0);
		}
		cookie.setPath("/");
		response.addCookie(cookie);
		
		//JSON 으로보내자 안보내도 되긴한데
		response.setStatus(HttpStatus.OK.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(Encode);
		
		Map<String,Object> responseData = new HashMap<>();
		responseData.put("message","로그인성공");
		
		// 🌟 권한에 따른 리다이렉트 경로 설정
		String redirectUrl = "/";
		boolean isRecruiter = authentication.getAuthorities().stream()
				.anyMatch(a -> a.getAuthority().equals("ROLE_RECRUITER"));
		
		if (isRecruiter) {
			redirectUrl = "/Recruiter/Main";
		}
		
		responseData.put("redirectUrl", redirectUrl);
		
		objectMapper.writeValue(response.getWriter(),responseData);
		
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
