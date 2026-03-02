package net.kumo.kumo.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
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

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				// 1. CSRF 보안 설정
				// (현재는 꺼져있으므로 POST 요청 시 403 에러는 안 납니다.)
				.csrf(AbstractHttpConfigurer::disable)

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
						
						// (3) 권한별 접근 제한
						.requestMatchers("/Recruiter/**").hasRole("RECRUITER")
						.requestMatchers("/Seeker/**").hasRole("SEEKER")

						// (4) API 접근 권한
						.requestMatchers("/api/check/**", "/api/**", "/api/mail/**").permitAll()
						.requestMatchers("/api/notifications/**").authenticated()

						// (5) 그 외 모든 요청은 인증 필요
						.anyRequest().authenticated())

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
						.deleteCookies("JSESSIONID"));

		return http.build();
	}

	// 비밀번호 암호화 빈
	@Bean
	public PasswordEncoder passwordEncoder() {
//		return new BCryptPasswordEncoder();
		return org.springframework.security.crypto.password.NoOpPasswordEncoder.getInstance();
		
	}
}