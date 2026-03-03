package net.kumo.kumo.domain.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
@Builder
public class JobApplicantGroupDTO {
    private Long jobId;            // 공고 테이블 고유 ID
    private String source;         // OSAKA or TOKYO
    private String jobTitle;       // 공고 제목
    private String status;         // 공고 상태 (RECRUITING, CLOSED 등)
    private int applicantCount;    // 해당 공고 총 지원자 수
    private LocalDateTime createdAt; // 최신순 정렬을 위한 등록일


    private List<ApplicationDTO.ApplicantResponse> applicants;
}