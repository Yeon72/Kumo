package net.kumo.kumo.repository;

import java.util.List;
import java.util.Optional;

import net.kumo.kumo.domain.entity.UserEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import net.kumo.kumo.domain.dto.projection.JobSummaryView;
import net.kumo.kumo.domain.entity.OsakaGeocodedEntity;

/**
 * 오사카 지역의 위치 좌표(Geocoded)가 포함된 구인 공고 데이터에 대한
 * 데이터베이스 접근을 담당하는 리포지토리 인터페이스입니다.
 */
@Repository
public interface OsakaGeocodedRepository extends BaseRepository<OsakaGeocodedEntity> {

    /**
     * 클라이언트 지도 뷰포트(Bounding Box) 범위 내에 존재하는 공고 최대 300건을 요약 뷰(Projection) 형태로 조회합니다.
     *
     * @param minLat 뷰포트 최소 위도
     * @param maxLat 뷰포트 최대 위도
     * @param minLng 뷰포트 최소 경도
     * @param maxLng 뷰포트 최대 경도
     * @return 범위 내 공고 요약 뷰 리스트
     */
    List<JobSummaryView> findTop300ByLatBetweenAndLngBetween(Double minLat, Double maxLat, Double minLng, Double maxLng);

    /**
     * 오사카 지역 내 기초 지자체(구, Ward)별 공고 등록 건수를 그룹화하여 집계합니다. (도넛 차트 통계용)
     *
     * @return [지역명, 건수] 형태의 Object 배열 리스트
     */
    @Query("SELECT o.wardJp, COUNT(o) FROM OsakaGeocodedEntity o GROUP BY o.wardJp HAVING o.wardJp IS NOT NULL")
    List<Object[]> countByWard();

    /**
     * 시스템 내부에 등록된 전체 공고 목록을 생성 일시 기준 내림차순(최신순)으로 조회합니다.
     *
     * @return 최신순으로 정렬된 공고 엔티티 리스트
     */
    List<OsakaGeocodedEntity> findAllByOrderByCreatedAtDesc();

    /**
     * 신규 공고 등록 시 고유 순번을 부여하기 위해 현재 테이블 내 최대 row_no 값을 조회합니다.
     *
     * @return 최대 row_no 값 (테이블이 비어있을 경우 null)
     */
    @Query("SELECT MAX(o.rowNo) FROM OsakaGeocodedEntity o")
    Integer findMaxRowNo();

    /**
     * 특정 회사(사업장)와 연관된 공고의 총 개수를 계산합니다. (회사 정보 삭제 전 제약 조건 확인용)
     *
     * @param companyId 확인할 회사의 고유 식별자
     * @return 해당 회사가 등록한 공고 수
     */
    long countByCompany_CompanyId(Long companyId);

    /**
     * 특정 사용자(이메일 계정)가 직접 작성하여 등록한 오사카 지역 공고 전체 목록을 조회합니다.
     *
     * @param email 작성자(구인자)의 이메일 계정
     * @return 작성자가 등록한 공고 엔티티 리스트
     */
    List<OsakaGeocodedEntity> findByUser_Email(String email);

    /**
     * 고유 식별 번호(datanum)를 기반으로 단일 공고의 상세 정보를 조회합니다.
     *
     * @param datanum 조회할 공고의 고유 번호
     * @return 일치하는 공고 엔티티 (존재하지 않을 경우 Optional.empty 반환)
     */
    Optional<OsakaGeocodedEntity> findByDatanum(Long datanum);

    /**
     * 특정 사용자가 등록한 모든 오사카 지역 공고 내역을 데이터베이스에서 삭제합니다. (회원 탈퇴 시 사용)
     *
     * @param user 삭제할 대상 사용자 엔티티
     */
    void deleteByUser(UserEntity user);
}