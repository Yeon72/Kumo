package net.kumo.kumo.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Formula;

/**
 * 오사카 지역 기반 구인 공고 중, 위치 좌표 데이터가
 * 없는(지도 마커 미제공) 공고 정보를 관리하는 엔티티 클래스입니다.
 */
@Entity
@Table(name = "osaka_no_geocoded")
@Getter
@Setter
public class OsakaNoGeocodedEntity extends BaseEntity {

    /** * 인터페이스 및 DTO 매핑 시 일관성을 유지하기 위한 가상 좌표 컬럼입니다.
     * DB 조회 시 강제로 NULL 값을 반환합니다.
     */
    @Formula("NULL")
    private Double lat;

    @Formula("NULL")
    private Double lng;
}