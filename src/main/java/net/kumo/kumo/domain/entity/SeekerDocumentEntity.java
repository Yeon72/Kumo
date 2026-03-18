package net.kumo.kumo.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 구직자(Seeker)가 업로드한 증빙 서류 및 포트폴리오 파일의
 * 메타데이터와 스토리지 경로를 관리하는 엔티티 클래스입니다.
 */
@Entity
@Table(name = "seeker_documents")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SeekerDocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long docId;

    /** 해당 서류를 업로드한 구직자 계정 매핑 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    /** 클라이언트가 업로드한 원본 파일명 */
    @Column(nullable = false)
    private String fileName;

    /** 파일이 실제 저장된 스토리지(로컬/S3) URL 경로 */
    @Column(nullable = false, length = 500)
    private String fileUrl;

    /** 파일 업로드 및 레코드 생성 일시 */
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime uploadDate;

    /**
     * 연관 관계 편의 메서드입니다.
     *
     * @param user 매핑할 대상 구직자 엔티티
     */
    public void setUser(UserEntity user) {
        this.user = user;
    }
}