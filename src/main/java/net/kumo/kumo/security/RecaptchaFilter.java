package net.kumo.kumo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.kumo.kumo.domain.entity.UserEntity;
import net.kumo.kumo.repository.UserRepository;
import net.kumo.kumo.util.RecaptchaService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Spring Security 인증 과정에서 사용자의 로그인 실패 횟수를 모니터링하고,
 * 특정 횟수 이상 실패 시 Google reCAPTCHA 검증 로직을 강제하는 보안 필터입니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecaptchaFilter extends OncePerRequestFilter {

    private final RecaptchaService recaptchaService;
    private final UserRepository userRepository;
    private final AuthenticationFailureHandler failureHandler;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if ("POST".equalsIgnoreCase(request.getMethod()) && "/loginProc".equals(request.getRequestURI())) {
            String email = request.getParameter("email");

            if (email != null) {
                Optional<UserEntity> userOptional = userRepository.findByEmail(email);

                if (userOptional.isPresent()) {
                    UserEntity user = userOptional.get();

                    if (user.getLoginFailCount() >= 5) {
                        String recaptchaResponse = request.getParameter("g-recaptcha-response");

                        if (!recaptchaService.verify(recaptchaResponse)) {
                            log.warn("[보안 인증 실패] reCAPTCHA 검증 실패 | Email: {}", email);

                            failureHandler.onAuthenticationFailure(request, response, new BadCredentialsException("CAPTCHA_FAILED"));
                            return;
                        }
                    }
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}