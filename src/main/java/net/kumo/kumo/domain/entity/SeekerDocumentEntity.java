package net.kumo.kumo.domain.entity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "seeker_documents")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 기본 생성자
@AllArgsConstructor
@Builder
public class SeekerDocumentEntity {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long docId;
	
	// 🌟 어떤 유저의 서류인지 연결 (다대일 관계)
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private UserEntity user;
	
	@Column(nullable = false)
	private String fileName;  // 원본 파일명 (예: 졸업증명서.png)
	
	@Column(nullable = false, length = 500)
	private String fileUrl;   // S3나 서버 저장 경로
	
	@CreationTimestamp // 🌟 INSERT 시 현재 시간 자동 저장
	@Column(updatable = false)
	private LocalDateTime uploadDate;
	
	// --- 연관 관계 편의 메서드 (선택 사항이지만 권장) ---
	public void setUser(UserEntity user) {
		this.user = user;
	}
}
