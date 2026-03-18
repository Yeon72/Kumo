package net.kumo.kumo.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * 구직자(Seeker)의 과거 직장 경력 및 업무 이력을 관리하는 엔티티 클래스입니다.
 */
@Entity
@Table(name = "seeker_careers")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class SeekerCareerEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long careerId;

    /** 해당 경력 이력을 소유한 구직자 계정 매핑 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    /** 근무했던 회사명 */
    @Column(length = 100, nullable = false)
    private String companyName;

    /** 소속 부서명 */
    @Column(length = 100)
    private String department;

    /** 근무 시작일 */
    private LocalDate startDate;

    /** 근무 종료일 (재직 중일 경우 null 또는 특정 처리) */
    private LocalDate endDate;

    /** 담당 업무 및 상세 경력 기술 본문 */
    @Column(columnDefinition = "TEXT")
    private String description;
}