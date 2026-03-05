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
//안녕하세요
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

        // 로그인 요청(/loginProc)일 때만 동작
        if ("POST".equalsIgnoreCase(request.getMethod()) && "/loginProc".equals(request.getRequestURI())) {
            String email = request.getParameter("email");

            if (email != null) {
                Optional<UserEntity> userOptional = userRepository.findByEmail(email);
                
                if (userOptional.isPresent()) {
                    UserEntity user = userOptional.get();
                    
                    // 실패 횟수가 5회 이상이면 리캡차 검증 수행
                    if (user.getLoginFailCount() >= 5) {
                        String recaptchaResponse = request.getParameter("g-recaptcha-response");
                        
                        if (!recaptchaService.verify(recaptchaResponse)) {
                            log.warn("[보안 인증 실패] reCAPTCHA 검증 실패 | Email: {}", email);
                            
                            // 검증 실패 시 FailureHandler를 통해 즉시 응답
                            failureHandler.onAuthenticationFailure(request, response, new BadCredentialsException("CAPTCHA_FAILED"));
                            return; // 더 이상 진행하지 않음
                        }
                    }
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
