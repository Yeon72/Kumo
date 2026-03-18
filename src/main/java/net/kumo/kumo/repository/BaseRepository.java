package net.kumo.kumo.repository;

import net.kumo.kumo.domain.dto.projection.JobSummaryView;
import net.kumo.kumo.domain.entity.BaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 구인 공고 기반 엔티티(OSAKA, TOKYO 등)의 공통 데이터 접근 및
 * 동적 쿼리 실행(JpaSpecificationExecutor)을 지원하는 최상위 리포지토리 인터페이스입니다.
 *
 * @param <T> BaseEntity를 상속받는 구인 공고 엔티티 타입
 */
@NoRepositoryBean
public interface BaseRepository<T extends BaseEntity> extends JpaRepository<T, Long>, JpaSpecificationExecutor<T> {

    /**
     * 공고 고유 식별 번호(datanum)를 기반으로 상세 정보를 조회합니다.
     *
     * @param datanum 조회할 공고의 고유 번호
     * @return 일치하는 공고 엔티티 (존재하지 않을 경우 Optional.empty 반환)
     */
    Optional<T> findByDatanum(Long datanum);

    /**
     * 입력된 문자열을 포함하는 회사명이 기재된 공고 목록을 요약 뷰(Projection) 형태로 조회합니다.
     *
     * @param name 검색할 회사명 문자열
     * @return 조건에 부합하는 공고 요약 뷰 리스트
     */
    List<JobSummaryView> findByCompanyNameContaining(String name);

    /**
     * 대시보드 통계 생성을 위해, 특정 기준 일시 이후에 등록된 신규 공고의 총 개수를 계산합니다.
     *
     * @param date 기준 일시
     * @return 신규 등록 공고 수
     */
    long countByCreatedAtAfter(LocalDateTime date);

    /**
     * 차트 데이터 생성을 위해, 특정 기준 일시 이후에 등록된 공고 목록을 조회합니다.
     *
     * @param date 기준 일시
     * @return 신규 등록 공고 엔티티 리스트
     */
    List<T> findByCreatedAtAfter(LocalDateTime date);
}