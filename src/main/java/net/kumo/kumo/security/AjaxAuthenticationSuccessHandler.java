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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Spring Security 비동기(AJAX) 로그인 요청 성공 시의 후속 처리(로그 기록,
 * 인증 실패 카운트 초기화, 쿠키 발급, 권한별 리다이렉트 응답)를 담당하는 핸들러 클래스입니다.
 */
@Component
@RequiredArgsConstructor
public class AjaxAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {

        String email = request.getParameter("email");
        String clientIp = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        String saveId = request.getParameter("saveId");
        String Encode = "UTF-8";

        Optional<UserEntity> UserOptional = userRepository.findByEmail(email);
        if (UserOptional.isPresent()) {
            UserEntity entity = UserOptional.get();
            entity.setLoginFailCount(0);
            userRepository.save(entity);
        }

        LoginHistoryEntity loginHistoryEntity = LoginHistoryEntity.builder()
                .email(email)
                .clientIp(clientIp)
                .userAgent(userAgent)
                .isSuccess(true)
                .failReason(null)
                .build();
        loginHistoryRepository.save(loginHistoryEntity);

        Cookie cookie = new Cookie("saveEmail", email);
        if(saveId != null && saveId.equals("on")){
            cookie.setMaxAge(60 * 60 * 24 * 30);
        }else {
            cookie.setMaxAge(0);
        }
        cookie.setPath("/");
        response.addCookie(cookie);

        response.setStatus(HttpStatus.OK.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(Encode);

        Map<String,Object> responseData = new HashMap<>();
        responseData.put("message", "로그인 성공");

        String redirectUrl = "/";
        boolean isRecruiter = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_RECRUITER"));

        if (isRecruiter) {
            redirectUrl = "/Recruiter/Main";
        }

        responseData.put("redirectUrl", redirectUrl);

        objectMapper.writeValue(response.getWriter(), responseData);
    }

    /**
     * 프록시 또는 로드밸런서 환경을 고려하여 클라이언트의 실제 IP 주소를 추출합니다.
     *
     * @param request HTTP 서블릿 요청 객체
     * @return 클라이언트 IP 문자열
     */
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