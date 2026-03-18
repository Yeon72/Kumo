package net.kumo.kumo.domain.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 구직자(Seeker)의 기본 이력서 노출 설정 및 자기소개(Self PR) 정보를
 * 1:1 관계로 관리하는 엔티티 클래스입니다.
 */
@Entity
@Table(name = "seeker_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeekerProfileEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long seekerProfileId;

    /** 프로필 설정을 소유한 구직자 계정 매핑 (1:1 고유 매핑) */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserEntity user;

    /** 경력 유형 (EXPERIENCED: 경력직, NEWCOMER: 신입) */
    @Column(length = 20)
    private String careerType;

    /** 자유 형식의 자기소개 본문 */
    @Column(columnDefinition = "TEXT")
    private String selfPr;

    /** 연락처 정보 공개 여부 (기본값: 공개) */
    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean contactPublic;

    /** 이력서 전체 공개 여부 (기본값: 공개) */
    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean isPublic;

    /** 구인자로부터의 스카우트 제의 수신 동의 여부 (기본값: 동의) */
    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean scoutAgree;

}