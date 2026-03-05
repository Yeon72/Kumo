package net.kumo.kumo.repository;

import java.util.List;
import java.util.Optional;

import net.kumo.kumo.domain.entity.UserEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import net.kumo.kumo.domain.dto.projection.JobSummaryView;
import net.kumo.kumo.domain.entity.OsakaGeocodedEntity;
import net.kumo.kumo.domain.entity.TokyoGeocodedEntity;

@Repository
public interface TokyoGeocodedRepository extends BaseRepository<TokyoGeocodedEntity> {

    // ★ 지도 쿼리 (Projection 사용)
    List<JobSummaryView> findTop300ByLatBetweenAndLngBetween(
            Double minLat, Double maxLat, Double minLng, Double maxLng);

    // ★ [추가] 도넛 차트용: 구(Ward)별 카운트 (도쿄는 컬럼명이 다름)
    @Query("SELECT t.wardCityJp, COUNT(t) FROM TokyoGeocodedEntity t GROUP BY t.wardCityJp HAVING t.wardCityJp IS NOT NULL")
    List<Object[]> countByWard();

    // 🌟 [추가] 최신순 조회 (createdAt이 엔티티에 있다면)
    List<OsakaGeocodedEntity> findAllByOrderByCreatedAtDesc();

    // 🌟 [추가] row_no 최대값 조회 (새 글 등록 시 번호 매기기용)
    @Query("SELECT MAX(o.rowNo) FROM OsakaGeocodedEntity o")
    Integer findMaxRowNo();

    // 회사 삭제 시 참조용
    long countByCompany_CompanyId(Long companyId);

    // 이메일을 기준으로 해당 유저가 쓴 도쿄 공고만 전부 가져오기
    List<TokyoGeocodedEntity> findByUser_Email(String email);

    // datanum으로 단일 공고 찾기
    Optional<TokyoGeocodedEntity> findByDatanum(Long datanum);
	
	void deleteByUser(UserEntity user);
}