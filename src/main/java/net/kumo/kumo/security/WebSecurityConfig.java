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

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class WebSecurityConfig implements WebMvcConfigurer {

	@Value("${file.upload.dir}")
	private String uploadDir;

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/uploads/**")
				.addResourceLocations("file:///" + uploadDir);
	}

	private final AjaxAuthenticationSuccessHandler successHandler;
	private final AjaxAuthenticationFailureHandler failureHandler;
	private final RecaptchaFilter recaptchaFilter; // 🌟 리캡차 필터 주입

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				// 1. CSRF 보안 설정
				.csrf(AbstractHttpConfigurer::disable)

				// 🌟 [추가] 리캡차 필터를 로그인 필터 앞에 배치
				.addFilterBefore(recaptchaFilter, UsernamePasswordAuthenticationFilter.class)

				// 2. 권한 설정 (authorizeHttpRequests -> 람다식)
				.authorizeHttpRequests((auth) -> auth
						// (1) 정적 리소스: css, js, images 등
						.requestMatchers("/css/**", "/js/**", "/images/**", "/error").permitAll()

						// (2) 로그인 없이 접근 가능한 페이지
						.requestMatchers("/map/api/**").permitAll()
						.requestMatchers("/", "/login", "/signup", "/join", "/join/**", "/info").permitAll()
						.requestMatchers("/map_non_login_view", "/FindId", "/FindPw", "/findIdProc", "/nickname",
								"/changePw", "/map/main", "/map/job-list-view")
						.permitAll()

						// (3) 권한별 접근 제한 (ADMIN은 두 곳 모두 접근 가능)
						.requestMatchers("/Recruiter/**").hasAnyRole("RECRUITER", "ADMIN")
						.requestMatchers("/Seeker/**").hasAnyRole("SEEKER", "ADMIN")

						// (4) API 접근 권한
						.requestMatchers("/api/check/**", "/api/**", "/api/mail/**").permitAll()
						.requestMatchers("/api/notifications/**").hasAnyRole("SEEKER", "RECRUITER", "ADMIN")

						// (5) 그 외 모든 요청은 인증 필요
						.anyRequest().hasAnyRole("SEEKER", "RECRUITER", "ADMIN"))

				// 3. 로그인 설정
				.formLogin((form) -> form
						.loginPage("/login")
						.loginProcessingUrl("/loginProc")
						.usernameParameter("email")
						.passwordParameter("password")
						.successHandler(successHandler)
						.failureHandler(failureHandler)
						.permitAll())

				// 4. 로그아웃 설정
				.logout((logout) -> logout
						.logoutUrl("/logout")
						.logoutSuccessUrl("/")
						.invalidateHttpSession(true)
						.deleteCookies("JSESSIONID"))
				
				
				// 5. 채팅창 팝업 출력관련 (iframe 허용)
				.headers((headers) -> headers
						.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
				); // 🌟 여기에 세미콜론(;) 추가!

		return http.build();
	}

	// 비밀번호 암호화 빈
	@Bean
	public PasswordEncoder passwordEncoder() {
//		return new BCryptPasswordEncoder();
		return org.springframework.security.crypto.password.NoOpPasswordEncoder.getInstance();
		
	}
}