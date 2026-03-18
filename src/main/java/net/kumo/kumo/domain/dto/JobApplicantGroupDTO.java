package net.kumo.kumo.domain.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 구인자의 대시보드(지원자 관리)에서 특정 구인 공고별로
 * 지원자 목록을 그룹화하여 렌더링하기 위한 래퍼(Wrapper) DTO 클래스입니다.
 */
@Getter
@Setter
@Builder
public class JobApplicantGroupDTO {

    /** 구인 공고 식별자 (ID) */
    private Long jobId;

    /** 구인 공고 출처 (OSAKA, TOKYO 등) */
    private String source;

    /** 한국어 구인 공고 제목 */
    private String jobTitle;

    /** 일본어 구인 공고 제목 */
    private String jobTitleJp;

    /** 공고 모집 상태 (RECRUITING, CLOSED 등) */
    private String status;

    /** 해당 공고의 누적 총 지원자 수 */
    private int applicantCount;

    /** 최신순 정렬 등을 위한 공고 등록 일시 */
    private LocalDateTime createdAt;

    /** 해당 공고에 지원한 지원자들의 상세 정보 리스트 */
    private List<ApplicationDTO.ApplicantResponse> applicants;

}