package net.kumo.kumo.config;

import java.time.Duration;
import java.util.Locale;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

/**
 * 다국어(i18n) 처리 및 정적 리소스 경로를 설정하는 Configuration 클래스입니다.
 */
@Configuration
public class LocaleConfig implements WebMvcConfigurer {

    /**
     * 클라이언트의 언어 설정을 유지하기 위한 Cookie 기반 LocaleResolver를 등록합니다.
     *
     * @return CookieLocaleResolver 객체 (기본값: KOREAN, 유지기간: 1일)
     */
    @Bean
    public LocaleResolver localeResolver() {
        CookieLocaleResolver resolver = new CookieLocaleResolver();
        resolver.setDefaultLocale(Locale.KOREAN);
        resolver.setCookieName("lang");
        resolver.setCookieMaxAge(Duration.ofDays(1));
        return resolver;
    }

    /**
     * HTTP 요청의 특정 파라미터 값을 감지하여 언어를 변경하는 Interceptor를 등록합니다.
     *
     * @return LocaleChangeInterceptor 객체 (감지 파라미터: "lang")
     */
    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");
        return interceptor;
    }

    /**
     * 커스텀 인터셉터를 Spring MVC 인터셉터 레지스트리에 추가합니다.
     *
     * @param registry InterceptorRegistry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }

    /**
     * 로컬 스토리지에 저장된 사용자 프로필 이미지 등의 정적 리소스 접근 경로를 매핑합니다.
     *
     * @param registry ResourceHandlerRegistry
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String actualPath = "file:" + System.getProperty("user.home") + "/kumo_uploads/profiles/";

        registry.addResourceHandler("/upload/profiles/**")
                .addResourceLocations(actualPath)
                .setCachePeriod(0); // 이미지 변경 시 브라우저 캐싱을 방지하고 즉시 반영
    }
}