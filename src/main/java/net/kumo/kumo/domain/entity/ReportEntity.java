package net.kumo.kumo.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

/**
 * 사용자가 접수한 특정 구인 공고에 대한 신고(Report) 내역을 관리하는 엔티티 클래스입니다.
 */
@Entity
@Table(name = "reports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long reportId;

    /** 신고를 접수한 사용자 정보 매핑 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private UserEntity reporter;

    /** 신고 대상 공고의 고유 식별자 */
    @Column(name = "target_post_id", nullable = false)
    private Long targetPostId;

    /** 신고 대상 공고의 데이터 출처 (OSAKA, TOKYO 등) */
    @Column(name = "target_source", nullable = false, length = 50)
    private String targetSource;

    /** 신고 사유 카테고리 분류 */
    @Column(name = "reason_category", nullable = false, length = 50)
    private String reasonCategory;

    /** 상세 신고 내용 본문 */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** * 신고 처리 상태 (PENDING, CLOSED, CHECKED 등).
     * 데이터베이스의 ENUM 타입과 유연하게 매핑하기 위해 문자열(String)로 관리합니다.
     */
    @Column(name = "status", nullable = false)
    @Builder.Default
    private String status = "PENDING";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * 신고의 처리 상태를 갱신합니다.
     *
     * @param newStatus 변경할 새로운 상태 문자열
     */
    public void updateStatus(String newStatus) {
        this.status = newStatus;
    }
}