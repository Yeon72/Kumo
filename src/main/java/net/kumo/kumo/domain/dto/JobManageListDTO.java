package net.kumo.kumo.domain.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * 구인자의 공고 관리 페이지에 노출될 개별 공고 요약 데이터를 담는 DTO 클래스입니다.
 */
@Getter
@Setter
@Data
@Builder
public class JobManageListDTO {

    private Long id;
    private Long datanum;

    private String title;
    private String titleJp;

    /** 모집 직무/포지션 명칭 */
    private String position;

    private String regionType;

    private String wage;
    private String wageJp;

    /** 담당자 연락처 정보 */
    private String contactPhone;

    private LocalDateTime createdAt;
    private String status;

}