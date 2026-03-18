package net.kumo.kumo.repository;

import net.kumo.kumo.domain.entity.TokyoNoGeocodedEntity;
import org.springframework.stereotype.Repository;

/**
 * 오사카 지역 기반 구인 공고 중, 위치 좌표 데이터가 없는(지도 마커 미제공)
 * 공고 데이터에 대한 데이터베이스 접근을 담당하는 리포지토리 인터페이스입니다.
 * 공통 데이터 접근 기능은 BaseRepository를 상속받아 활용합니다.
 */
@Repository
public interface TokyoNoGeocodedRepository extends BaseRepository<TokyoNoGeocodedEntity> {

}