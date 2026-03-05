package net.kumo.kumo.util;
// 안녕하세요
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class RecaptchaService {

    @Value("${google.recaptcha.secret-key}")
    private String secretKey;

    private static final String RECAPTCHA_VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

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
            log.debug("reCAPTCHA 검증 결과: {}", success);
            
            return success;
        } catch (Exception e) {
            log.error("reCAPTCHA 검증 중 오류 발생", e);
            return false;
        }
    }
}
