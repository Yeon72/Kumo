package net.kumo.kumo.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import net.kumo.kumo.domain.entity.Enum.ApplicationStatus;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 구직자의 구인 공고 지원 내역(Application)을 관리하는 엔티티 클래스입니다.
 * 중복 지원 방지를 위해 '공고 출처, 공고 식별자, 구직자 식별자' 조합으로 유니크 제약 조건을 설정합니다.
 */
@Entity
@Table(
        name = "applications",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_application_source_post_seeker",
                        columnNames = {"target_source", "target_post_id", "seeker_id"}
                )
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ApplicationEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "app_id")
    private Long id;

    @Column(name = "target_source", nullable = false, length = 20)
    private String targetSource;

    @Column(name = "target_post_id", nullable = false)
    private Long targetPostId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seeker_id", nullable = false)
    private UserEntity seeker;

    /** 지원 진행 상태 (데이터베이스에는 문자열로 저장됨) */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30)
    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.APPLIED;

    @CreationTimestamp
    @Column(name = "applied_at", updatable = false)
    private LocalDateTime appliedAt;

}