package net.kumo.kumo.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.kumo.kumo.domain.entity.ReportEntity;
import java.time.LocalDateTime;

/**
 * 사용자 신고 내역을 클라이언트와 송수신하고, 관리자 대시보드에
 * 렌더링하기 위한 데이터 전송 객체(DTO) 클래스입니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportDTO {

    private Long reportId;

    /** 신고자 식별자 (ID) */
    private Long reporterId;

    /** 신고자 이메일 계정 (Service 계층에서 매핑됨) */
    private String reporterEmail;

    /** 신고 대상 구인 공고 식별자 */
    private Long targetPostId;

    /** 신고 대상 공고 출처 (OSAKA, TOKYO 등) */
    private String targetSource;

    /** 신고 대상 공고 제목 (Service 계층에서 매핑됨) */
    private String targetPostTitle;

    /** 신고 사유 카테고리 명칭 */
    private String reasonCategory;

    /** 상세 신고 내용 (본문) */
    private String description;

    /** 신고 처리 상태 (접수, 처리중, 완료 등) */
    private String status;

    private LocalDateTime createdAt;

    /**
     * ReportEntity 객체를 기반으로 ReportDTO 객체를 생성하여 반환합니다.
     * 연관 관계를 통해 필요한 식별자를 안전하게 추출합니다.
     *
     * @param entity 변환할 원본 신고 내역 엔티티
     * @return 매핑이 완료된 ReportDTO 객체
     */
    public static ReportDTO fromEntity(ReportEntity entity) {

        Long rId = (entity.getReporter() != null) ? entity.getReporter().getUserId() : null;

        return ReportDTO.builder()
                .reportId(entity.getReportId())
                .reporterId(rId)
                .reporterEmail("-")
                .targetPostId(entity.getTargetPostId())
                .targetSource(entity.getTargetSource())
                .targetPostTitle("-")
                .description(entity.getDescription())
                .reasonCategory(entity.getReasonCategory())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}