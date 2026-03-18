package net.kumo.kumo.domain.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 기존 구인 공고의 상세 정보 조회 및 수정 요청 데이터를
 * 다국어 포맷으로 매핑하고 송수신하기 위한 DTO 클래스입니다.
 */
@Getter
@Setter
@ToString
public class JobPostingRequestDTO {

    /** 공고 엔티티 기본 키 (수정 프로세스 시 필수) */
    private Long id;

    /** 공고 고유 식별 번호 (datanum) */
    private Long datanum;

    private String title;
    private String titleJp;

    private String position;
    private String positionJp;

    private String jobDescription;
    private String jobDescriptionJp;

    private String contactPhone;
    private String contactPhoneJp;

    private String notes;
    private String notesJp;

    /** 급여 지급 유형 (예: HOURLY, DAILY 등) */
    private String salaryType;

    /** 책정된 급여 금액 */
    private Integer salaryAmount;

    /** 연관된 회사의 고유 식별자 */
    private Long companyId;

}