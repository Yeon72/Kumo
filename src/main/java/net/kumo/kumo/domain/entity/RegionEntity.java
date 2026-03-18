package net.kumo.kumo.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import net.kumo.kumo.domain.entity.Enum.RegionType;

/**
 * 행정 구역(Region) 데이터를 계층적(Hierarchical) 구조로 관리하는 엔티티 클래스입니다.
 * 자기 참조(Self-Referencing)를 통해 상/하위 행정 구역 관계를 표현합니다.
 */
@Entity
@Table(name = "regions")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class RegionEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "region_id")
    private Long id;

    /** 상위 행정 구역을 가리키는 자기 참조 외래키 (최상위 구역일 경우 null) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private RegionEntity parent;

    /** 행정 구역의 분류 단위 (PREFECTURE, CITY, WARD 등) */
    @Enumerated(EnumType.STRING)
    @Column(name = "region_type", nullable = false, length = 20)
    private RegionType regionType;

    /** 시스템 외부 연동 등을 위한 고유 지역 코드 */
    @Column(name = "region_code", length = 20)
    private String regionCode;

    /** 지역의 기본 표기명 (한자 또는 영문 등) */
    @Column(nullable = false, length = 100)
    private String name;

    /** 지역의 가나(일본어 발음) 표기명 */
    @Column(name = "name_kana", length = 100)
    private String nameKana;
}