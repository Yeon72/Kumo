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

    // 🌟 변경된 코드 (이 두 줄을 추가하세요!)
    @Column(name = "target_post_id", nullable = false)
    private Long targetPostId;

    @Column(name = "target_source", nullable = false, length = 20)
    private String targetSource;

    // ==========================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seeker_id", nullable = false)
    private UserEntity seeker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruiter_id", nullable = false)
    private UserEntity recruiter;

    // 채팅 목록에서 최신 메시지를 보여주기 위한 컬럼
    @Column(length = 1000)
    private String lastMessage;

    private LocalDateTime lastMessageAt;

    @CreationTimestamp // 🌟 저장 시점에 자동으로 현재 시간 주입!
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
