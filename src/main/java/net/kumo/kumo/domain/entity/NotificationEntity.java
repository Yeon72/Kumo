package net.kumo.kumo.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

/**
 * 플랫폼 시스템 내에서 발생하여 사용자에게 발송되는 개별 알림(Notification) 내역을 관리하는 엔티티 클래스입니다.
 */
@Entity
@Table(name = "notifications")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long id;

    /** 알림을 수신할 대상 사용자 매핑 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    /** 발생한 알림의 카테고리/유형 */
    @Enumerated(EnumType.STRING)
    @Column(name = "notify_type", nullable = false, columnDefinition = "VARCHAR(30)")
    private Enum.NotificationType notifyType;

    /** 알림의 다국어 메시지 키 또는 제목 텍스트 */
    @Column(nullable = true)
    private String title;

    /** 알림 상세 내용 본문 */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /** 알림 클릭 시 리다이렉트될 라우팅 대상 URL */
    private String targetUrl;

    /** 사용자의 알림 조회(읽음) 여부 */
    @Builder.Default
    @Column(name = "is_read")
    private boolean isRead = false;

    /** 알림 생성 일시 */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}