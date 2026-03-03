package net.kumo.kumo.controller;

import net.kumo.kumo.domain.dto.TranslationRequestDTO;
import net.kumo.kumo.domain.dto.TranslationResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * DeepL API를 이용한 실시간 번역 컨트롤러
 * DTO를 활용하여 타입 안정성을 확보하고 경고 메시지를 제거했습니다.
 */
@RestController
@RequestMapping("/api/translate")
public class DeepLController {

    @Value("${deepl.api.key}")
    private String apiKey;

    @Value("${deepl.api.url}")
    private String apiUrl;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TranslationResponseDTO> translate(@RequestBody TranslationRequestDTO request) {
        // 1. 매개변수 타입을 Map에서 TranslationRequest DTO로 변경했습니다.

        RestTemplate restTemplate = new RestTemplate();

        // 2. 헤더 설정: JSON 통신을 명시하여 컨버터 오류를 방지합니다.
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "DeepL-Auth-Key " + apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        // 3. DeepL API 규격에 맞게 요청 바디 구성 (text는 배열로 전달)
        Map<String, Object> body = new HashMap<>();
        body.put("text", request.getText());
        body.put("target_lang", request.getTarget_lang());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            // 4. 핵심 변경: 응답을 Map이 아닌 TranslationResponse DTO로 직접 받습니다.
            // 이 수정을 통해 'Unchecked cast' 및 'Raw type' 경고가 모두 해결됩니다.
            TranslationResponseDTO response = restTemplate.postForObject(apiUrl, entity, TranslationResponseDTO.class);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}