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
 * DeepL 외부 API를 활용하여 클라이언트의 번역 요청(Tagengo)을 중계하는 API Controller 클래스입니다.
 */
@RestController
@RequestMapping("/api/translate")
public class DeepLController {

    @Value("${deepl.api.key}")
    private String apiKey;

    @Value("${deepl.api.url}")
    private String apiUrl;

    /**
     * 텍스트와 목표 언어 코드를 수신하여 DeepL API로 전송한 뒤 번역 결과를 반환합니다.
     *
     * @param request 번역할 텍스트 및 타겟 언어 정보가 담긴 DTO
     * @return 번역 결과가 포함된 응답 객체 (에러 발생 시 500 상태 코드 반환)
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TranslationResponseDTO> translate(@RequestBody TranslationRequestDTO request) {

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "DeepL-Auth-Key " + apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        Map<String, Object> body = new HashMap<>();
        body.put("text", request.getText());
        body.put("target_lang", request.getTarget_lang());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            TranslationResponseDTO response = restTemplate.postForObject(apiUrl, entity, TranslationResponseDTO.class);
            return ResponseEntity.ok(response);

        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            System.out.println("[DeepL API Error] 통신 상태 코드 이상 및 번역 거부 처리");
            System.out.println("Detail: " + e.getResponseBodyAsString());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();

        } catch (Exception e) {
            System.out.println("[DeepL System Error] 스프링 내부 통신 장애 발생");
            System.out.println("Detail: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}