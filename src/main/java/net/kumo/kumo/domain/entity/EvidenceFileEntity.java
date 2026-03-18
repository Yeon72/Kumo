package net.kumo.kumo.domain.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 구인자 사업자 인증 서류 등 사용자가 업로드한 증빙 파일의
 * 메타데이터를 관리하는 엔티티 클래스입니다.
 */
@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "evidence_file")
public class EvidenceFileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 서버 스토리지에 저장된 고유 식별 파일명 (예: UUID_원본파일명.ext) */
    @Column(nullable = false)
    private String fileName;

    /** 파일의 논리적 분류 타입 (예: EVIDENCE, PROFILE 등) */
    private String fileType;

    /** 파일을 업로드한 대상 사용자 정보 매핑 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

}