package net.kumo.kumo.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

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

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
}

//package net.kumo.kumo.domain.entity;
//
//import jakarta.persistence.*;
//import lombok.*;
//import org.hibernate.annotations.CreationTimestamp;
//
//import java.time.LocalDateTime;
//
//@Entity
//@Table(name = "chat_rooms")
//@Getter
//@Setter
//@NoArgsConstructor
//@AllArgsConstructor
//@Builder
//public class ChatRoomEntity {
//
//	@Id
//	@GeneratedValue(strategy = GenerationType.IDENTITY)
//	@Column(name = "room_id")
//	private Long id;
//
//	@ManyToOne(fetch = FetchType.LAZY)
//	@JoinColumn(name = "job_post_id", nullable = false)
//	private JobPostingEntity jobPosting;
//
//	@ManyToOne(fetch = FetchType.LAZY)
//	@JoinColumn(name = "seeker_id", nullable = false)
//	private UserEntity seeker;
//
//	@ManyToOne(fetch = FetchType.LAZY)
//	@JoinColumn(name = "recruiter_id", nullable = false)
//	private UserEntity recruiter;
//
//	@Column(name = "last_message", length = 500)
//	private String lastMessage;
//
//	@Column(name = "last_message_at")
//	private LocalDateTime lastMessageAt;
//
//	@CreationTimestamp
//	@Column(name = "created_at", updatable = false)
//	private LocalDateTime createdAt;
//}
