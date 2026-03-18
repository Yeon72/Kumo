package net.kumo.kumo.domain.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 구직자(Seeker)가 희망하는 근무 조건(지역, 직무, 급여 등)을
 * 1:1 관계로 관리하는 엔티티 클래스입니다.
 */
@Entity
@Table(name = "seeker_desired_conditions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeekerDesiredConditionEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long conditionId;

    /** 근무 조건을 설정한 구직자 계정 매핑 (1:1 고유 매핑) */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserEntity user;

    /** 희망 근무 광역 지자체 (도/부/현 등) */
    @Column(length = 50)
    private String locationPrefecture;

    /** 희망 근무 기초 지자체 (시/구/정/촌 등) */
    @Column(length = 50)
    private String locationWard;

    /** 희망 직무 및 포지션 */
    @Column(length = 100)
    private String desiredJob;

    /** 희망 급여 형태 (예: HOURLY, DAILY, MONTHLY 등) */
    @Column(length = 20)
    private String salaryType;

    /** 희망 급여 금액 또는 협의 여부 */
    private String desiredSalary;

    /** 희망 근무 기간 */
    @Column(length = 50)
    private String desiredPeriod;

}