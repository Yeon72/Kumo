package net.kumo.kumo.domain.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * DeepL 등의 외부 번역 API로 텍스트 번역을 요청할 때
 * 클라이언트로부터 수신받는 페이로드(Payload) DTO 클래스입니다.
 */
@Getter
@Setter
public class TranslationRequestDTO {

    /** 번역을 요청할 원본 텍스트 리스트 */
    private List<String> text;

    /** 번역될 타겟 언어 코드 (예: "JA", "KO") */
    private String target_lang;

}