package net.kumo.kumo.domain.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

/**
 * 외부 번역 API로부터 수신한 번역 결과 데이터를
 * 클라이언트에게 반환하기 위한 응답 DTO 클래스입니다.
 */
@Getter
@Setter
public class TranslationResponseDTO {

    /** 번역 완료된 데이터 리스트 */
    private List<TranslationData> translations;

    /**
     * JSON 직렬화/역직렬화(Jackson)를 위한 내부 정적(Static) DTO 클래스입니다.
     */
    @Getter
    @Setter
    public static class TranslationData {
        private String text;
    }
}