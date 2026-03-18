package net.kumo.kumo.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.RequiredArgsConstructor;

/**
 * 애플리케이션의 웹 보안 정책(Spring Security), 정적 리소스 핸들링,
 * URL별 접근 권한 및 인증/인가 필터 체인을 전역적으로 설정하는 Configuration 클래스입니다.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class WebSecurityConfig implements WebMvcConfigurer {

    @Value("${file.upload.dir}")
    private String uploadDir;

    private final AjaxAuthenticationSuccessHandler successHandler;
    private final AjaxAuthenticationFailureHandler failureHandler;
    private final RecaptchaFilter recaptchaFilter;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String path = uploadDir.endsWith("/") ? uploadDir : uploadDir + "/";

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:///" + path);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)

                .addFilterBefore(recaptchaFilter, UsernamePasswordAuthenticationFilter.class)

                .authorizeHttpRequests((auth) -> auth
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/error", "/uploads/**").permitAll()

                        .requestMatchers("/map/api/**").permitAll()
                        .requestMatchers("/", "/login", "/signup", "/join", "/join/**", "/info").permitAll()
                        .requestMatchers("/map_non_login_view", "/FindId", "/FindPw", "/findIdProc", "/nickname",
                                "/changePw", "/map/main", "/map/job-list-view").permitAll()

                        .requestMatchers("/Recruiter/**").hasAnyRole("RECRUITER", "ADMIN")
                        .requestMatchers("/Seeker/**").hasAnyRole("SEEKER", "ADMIN")

                        .requestMatchers("/api/check/**", "/api/**", "/api/mail/**").permitAll()
                        .requestMatchers("/api/notifications/**").hasAnyRole("SEEKER", "RECRUITER", "ADMIN")

                        .anyRequest().hasAnyRole("SEEKER", "RECRUITER", "ADMIN"))

                .formLogin((form) -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/loginProc")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler(successHandler)
                        .failureHandler(failureHandler)
                        .permitAll())

                .logout((logout) -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID"))

                .headers((headers) -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                );

        return http.build();
    }

    /**
     * 비밀번호 암호화 인코더 빈을 등록합니다.
     * (현재는 마이그레이션 또는 테스트 목적으로 평문 기반의 NoOpPasswordEncoder를 사용합니다.)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return org.springframework.security.crypto.password.NoOpPasswordEncoder.getInstance();
    }
}