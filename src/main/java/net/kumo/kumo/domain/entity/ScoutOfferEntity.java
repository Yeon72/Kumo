package net.kumo.kumo.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

/**
 * 구인자가 특정 구직자에게 발송한 스카우트 제의(Scout Offer) 내역 및
 * 수락/거절 상태를 관리하는 엔티티 클래스입니다.
 */
@Entity
@Table(name = "scout_offers")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ScoutOfferEntity {

    /** 스카우트 제의 처리 상태 열거형 */
    public enum ScoutStatus {
        PENDING, ACCEPTED, REJECTED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long scoutId;

    /** 스카우트 제의를 발송한 구인자 정보 매핑 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruiter_id", nullable = false)
    private UserEntity recruiter;

    /** 스카우트 제의를 수신한 구직자 정보 매핑 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seeker_id", nullable = false)
    private UserEntity seeker;

    /** 제의 수락 여부 상태 (기본값: PENDING) */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private ScoutStatus status = ScoutStatus.PENDING;

    /** 스카우트 제의 발송 일시 */
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

}