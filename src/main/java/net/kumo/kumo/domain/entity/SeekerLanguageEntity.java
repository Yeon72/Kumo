package net.kumo.kumo.domain.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 구직자(Seeker)가 보유한 외국어 능력 및 어학 수준 정보를 관리하는 엔티티 클래스입니다.
 */
@Entity
@Table(name = "seeker_languages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeekerLanguageEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long langId;

    /** 어학 정보를 소유한 구직자 계정 매핑 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    /** 구사 가능한 외국어 명칭 (예: 영어, 일본어 등) */
    @Column(length = 50, nullable = false)
    private String language;

    /** 어학 수준 (예: ADVANCED, INTERMEDIATE, BEGINNER 등) */
    @Column(length = 50)
    private String level;

}