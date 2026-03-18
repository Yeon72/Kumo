package net.kumo.kumo.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

/**
 * 사용자별 계정 로그인 시도 이력 및 보안 로깅 정보를 관리하는 엔티티 클래스입니다.
 */
@Entity
@Table(name = "login_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class LoginHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long historyId;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, length = 50)
    private String clientIp;

    /** 로그인 시도 기기의 브라우저 및 운영체제 등 User-Agent 정보 */
    private String userAgent;

    /** 인증 성공 여부 (성공: true, 실패: false) */
    @Column(nullable = false)
    private boolean isSuccess;

    /** 실패 시 기록되는 사유 메시지 */
    private String failReason;

    /** 로그인 시도 일시 */
    @CreationTimestamp
    private LocalDateTime attemptTime;
}