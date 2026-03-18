package net.kumo.kumo.domain.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 사용자가 등록한 프로필 이미지의 스토리지 경로 및 메타데이터를
 * 관리하는 엔티티 클래스입니다.
 */
@Entity
@Table(name = "profile_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileImageEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 클라이언트로부터 업로드된 원본 이미지 파일명 */
    private String originalFileName;

    /** 서버 스토리지에 중복 방지를 위해 저장된 UUID 결합 파일명 */
    private String storedFileName;

    /** 브라우저에서 이미지 렌더링 시 접근할 서버 매핑 URL 경로 */
    private String fileUrl;

    /** 저장된 이미지 파일의 총 바이트(Byte) 크기 */
    private Long fileSize;

    @ToString.Exclude
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

}