package net.kumo.kumo.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Google reCAPTCHA v2 API 서버와 통신하여, 클라이언트로부터 전송된
 * 캡차 검증 토큰의 무결성 및 인증 성공 여부를 확인하는 서비스 유틸리티입니다.
 */
@Slf4j
@Service
public class RecaptchaService {

    @Value("${google.recaptcha.secret-key}")
    private String secretKey;

    private static final String RECAPTCHA_VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

    /**
     * 클라이언트가 제출한 캡차 토큰을 구글 공식 API 서버로 전송하여 봇 여부 판별 결과를 검증합니다.
     *
     * @param response 클라이언트 측 캡차 토큰 (g-recaptcha-response)
     * @return 검증 성공 여부 (성공 시 true)
     */
    public boolean verify(String response) {
        if (response == null || response.isEmpty()) {
            return false;
        }

        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = RECAPTCHA_VERIFY_URL + "?secret=" + secretKey + "&response=" + response;

            @SuppressWarnings("unchecked")
            Map<String, Object> body = restTemplate.getForObject(url, Map.class);

            if (body == null) return false;

            boolean success = (Boolean) body.get("success");
            log.debug("reCAPTCHA API 원격 검증 완료 결과: {}", success);

            return success;
        } catch (Exception e) {
            log.error("reCAPTCHA API 원격 검증 통신 오류 발생", e);
            return false;
        }
    }
}