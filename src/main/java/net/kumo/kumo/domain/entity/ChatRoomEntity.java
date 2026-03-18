package net.kumo.kumo.domain.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 1:1 매칭 채팅을 위한 채팅방(ChatRoom) 세션 정보와
 * 참여자, 최신 메시지 스냅샷 등을 관리하는 엔티티 클래스입니다.
 */
@Entity
@Table(name = "chat_rooms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class ChatRoomEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** 대화의 시발점이 된 타겟 구인 공고 식별자 */
    @Column(name = "target_post_id", nullable = false)
    private Long targetPostId;

    /** 대상 공고 데이터 출처 지역 (예: OSAKA) */
    @Column(name = "target_source", nullable = false, length = 20)
    private String targetSource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seeker_id", nullable = false)
    private UserEntity seeker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruiter_id", nullable = false)
    private UserEntity recruiter;

    /** 채팅 목록 렌더링을 위해 캐싱된 가장 최근 메시지 내용 */
    @Column(length = 1000)
    private String lastMessage;

    private LocalDateTime lastMessageAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

}