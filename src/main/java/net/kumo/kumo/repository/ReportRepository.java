package net.kumo.kumo.repository;

import net.kumo.kumo.domain.entity.ReportEntity;
import net.kumo.kumo.domain.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 사용자가 접수한 구인 공고 신고(Report) 내역에 대한 데이터베이스 접근을 담당하는 리포지토리 인터페이스입니다.
 */
@Repository
public interface ReportRepository extends JpaRepository<ReportEntity, Long> {

    /**
     * 전체 신고 내역 목록을 생성 일시 기준 내림차순(최신순)으로 조회합니다.
     * 주로 관리자 대시보드에서 신고 목록을 렌더링할 때 사용됩니다.
     *
     * @return 최신순으로 정렬된 신고 엔티티 리스트
     */
    List<ReportEntity> findAllByOrderByCreatedAtDesc();

    /**
     * 특정 처리 상태(예: PENDING, RESOLVED 등)에 해당하는 신고 내역만을 최신순으로 필터링하여 조회합니다.
     *
     * @param status 조회할 신고 처리 상태 문자열
     * @return 조건에 부합하는 신고 엔티티 리스트
     */
    List<ReportEntity> findByStatusOrderByCreatedAtDesc(String status);

    /**
     * 특정 사용자가 동일한 대상 공고에 대해 이미 신고를 접수했는지 여부를 확인합니다.
     * 무분별한 중복 신고 접수를 방지하기 위해 사용됩니다.
     *
     * @param reporterId   신고 접수 여부를 확인할 사용자 식별자
     * @param targetPostId 신고 대상 공고 식별자
     * @return 중복 신고 내역 존재 여부
     */
    @Query("SELECT COUNT(r) > 0 FROM ReportEntity r WHERE r.reporter.userId = :reporterId AND r.targetPostId = :targetPostId")
    boolean existsByReporterIdAndTargetPostId(
            @Param("reporterId") Long reporterId,
            @Param("targetPostId") Long targetPostId);

    /**
     * 특정 구인 공고가 시스템에서 삭제될 때, 해당 공고를 타겟으로 접수된 모든 신고 내역을 일괄 삭제합니다.
     * 엔티티 간 외래키(FK) 매핑이 느슨할 경우 고아 데이터를 정리하기 위해 서비스 계층에서 명시적으로 호출됩니다.
     *
     * @param targetSource 삭제 대상 공고의 데이터 출처
     * @param targetPostId 삭제 대상 공고 식별자
     */
    void deleteByTargetSourceAndTargetPostId(String targetSource, Long targetPostId);

    /**
     * 특정 사용자가 접수한 모든 신고 내역을 데이터베이스에서 삭제합니다. (회원 탈퇴 시 사용)
     *
     * @param user 삭제할 대상 신고자(사용자) 엔티티
     */
    void deleteByReporter(UserEntity user);
}