package net.kumo.kumo.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.kumo.kumo.domain.entity.LoginHistoryEntity;
import net.kumo.kumo.domain.entity.UserEntity;
import net.kumo.kumo.repository.LoginHistoryRepository;
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

/**
 * Spring Security 비동기(AJAX) 로그인 요청 실패 시 예외 처리와
 * 응답 데이터(JSON) 생성, 보안 감사 로그 기록을 담당하는 핸들러 클래스입니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AjaxAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final MessageSource messageSource;
    private final LocaleResolver localeResolver;
    private final LoginHistoryRepository loginHistoryRepository;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException, ServletException {

        String email = request.getParameter("email");
        String clientIp = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        Locale locale = localeResolver.resolveLocale(request);

        String errorMessage = "";
        int failCount = 0;
        boolean showCaptcha = false;
        String failReasonLog = "";

        if (email != null) {
            if ("CAPTCHA_FAILED".equals(exception.getMessage())) {
                showCaptcha = true;
                failReasonLog = "리캡차 인증 실패";
                errorMessage = messageSource.getMessage("login.fail.captcha", null, locale);
                log.warn("[보안 경고] 리캡차 미인증 또는 검증 실패 | Email: {}", email);
            } else {
                Optional<UserEntity> userOptional = userRepository.findByEmail(email);

                if (userOptional.isPresent()) {
                    UserEntity user = userOptional.get();
                    user.increaseFailCount();
                    userRepository.save(user);

                    failCount = user.getLoginFailCount();

                    if (failCount >= 5) {
                        showCaptcha = true;
                        failReasonLog = "비밀번호 5회 이상 오류 (캡차 요구)";
                        errorMessage = messageSource.getMessage("login.fail.captcha", null, locale);
                        log.warn("[보안 경고] 5회 이상 실패 계정 감지 | IP: {} | Email: {}", clientIp, email);
                    } else {
                        failReasonLog = "비밀번호 불일치 (" + failCount + "회)";
                        errorMessage = messageSource.getMessage("login.fail.mismatch", null, locale);
                        log.warn("[로그인 실패] 비밀번호 불일치 | IP: {} | Email: {} | 횟수: {}", clientIp, email, failCount);
                    }
                } else {
                    failReasonLog = "존재하지 않는 계정 시도";
                    log.warn("[로그인 실패] 존재하지 않는 계정 | IP: {} | 입력한 Email: {}", clientIp, email);
                    errorMessage = messageSource.getMessage("login.fail.mismatch", null, locale);
                }
            }
        } else {
            failReasonLog = "이메일 파라미터 누락 (비정상 요청)";
            log.warn("[로그인 실패] 파라미터 없음 | IP: {}", clientIp);
            errorMessage = messageSource.getMessage("login.fail.mismatch", null, locale);
        }

        try {
            LoginHistoryEntity history = LoginHistoryEntity.builder()
                    .email(email == null ? "unknown" : email)
                    .clientIp(clientIp)
                    .userAgent(userAgent)
                    .isSuccess(false)
                    .failReason(failReasonLog)
                    .build();

            loginHistoryRepository.save(history);
        } catch (Exception e) {
            log.error("로그인 실패 이력 DB 저장 중 오류 발생", e);
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> data = new HashMap<>();
        data.put("code", "LOGIN_FAILED");
        data.put("message", errorMessage);
        data.put("failCount", failCount);
        data.put("showCaptcha", showCaptcha);

        objectMapper.writeValue(response.getWriter(), data);
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