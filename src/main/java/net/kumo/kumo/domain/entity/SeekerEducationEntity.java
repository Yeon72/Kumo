package net.kumo.kumo.domain.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 구직자(Seeker)의 학위 및 학력 정보를 관리하는 엔티티 클래스입니다.
 */
@Entity
@Table(name = "seeker_educations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeekerEducationEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long eduId;

    /** 학력 정보를 소유한 구직자 계정 매핑 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    /** 교육 수준 (예: 고졸, 대졸 등) */
    @Column(length = 50, nullable = false)
    private String educationLevel;

    /** 교육 기관(학교) 명칭 */
    @Column(length = 100, nullable = false)
    private String schoolName;

    /** 전공 및 학위 명칭 */
    @Column(length = 100)
    private String major;

    /** 학위 이수 상태 (예: GRADUATED, EXPECTED 등) */
    @Column(length = 20)
    private String status;
}