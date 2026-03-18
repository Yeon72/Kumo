package net.kumo.kumo.domain.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 구직자(Seeker)가 보유한 자격증 취득 내역을 관리하는 엔티티 클래스입니다.
 */
@Entity
@Table(name = "seeker_certificates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeekerCertificateEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long certId;

    /** 자격증 정보를 소유한 구직자 계정 매핑 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    /** 취득한 자격증 명칭 */
    @Column(length = 100, nullable = false)
    private String certName;

    /** 자격증 취득 연도 (예: "2024") */
    @Column(length = 4)
    private String acquisitionYear;

    /** 자격증 발급 기관명 */
    @Column(length = 100)
    private String issuer;
}