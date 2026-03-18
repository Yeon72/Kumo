package net.kumo.kumo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import net.kumo.kumo.domain.entity.ApplicationEntity;
import net.kumo.kumo.domain.entity.UserEntity;

/**
 * 구인 공고 지원 내역(Application)에 대한 데이터베이스 접근을 담당하는 리포지토리 인터페이스입니다.
 */
@Repository
public interface ApplicationRepository extends JpaRepository<ApplicationEntity, Long> {

    /**
     * 특정 구직자가 지정된 공고에 이미 지원했는지 여부를 확인하여 중복 지원을 방지합니다.
     *
     * @param targetSource 지원 대상 공고의 데이터 출처
     * @param targetPostId 지원 대상 공고 식별자
     * @param seeker       지원 여부를 확인할 구직자 엔티티
     * @return 지원 내역 존재 여부
     */
    boolean existsByTargetSourceAndTargetPostIdAndSeeker(String targetSource, Long targetPostId, UserEntity seeker);

    /**
     * 구직자 본인의 전체 지원 내역 목록을 최신순으로 조회합니다. (마이페이지용)
     *
     * @param seeker 조회를 요청한 구직자 엔티티
     * @return 구직자의 지원 내역 리스트
     */
    List<ApplicationEntity> findBySeekerOrderByAppliedAtDesc(UserEntity seeker);

    /**
     * 특정 구인 공고에 지원한 지원자 목록을 최신순으로 조회합니다. (구인자 관리용)
     *
     * @param targetSource 조회 대상 공고의 데이터 출처
     * @param targetPostId 조회 대상 공고 식별자
     * @return 해당 공고의 지원 내역 리스트
     */
    List<ApplicationEntity> findByTargetSourceAndTargetPostIdOrderByAppliedAtDesc(String targetSource, Long targetPostId);

    /**
     * 다수의 구인 공고 식별자에 해당하는 지원 내역을 한 번에 조회합니다.
     *
     * @param targetSource 조회 대상 공고의 데이터 출처
     * @param targetPostIds 조회 대상 공고 식별자 리스트
     * @return 일치하는 지원 내역 리스트
     */
    List<ApplicationEntity> findByTargetSourceAndTargetPostIdIn(String targetSource, List<Long> targetPostIds);

    /**
     * 구인자의 특정 공고들에 접수된 지원 내역 중, 아직 확인하지 않은(APPLIED 상태) 지원자 수를 계산합니다.
     *
     * @param source  조회 대상 공고의 데이터 출처
     * @param postIds 조회 대상 공고 식별자 리스트
     * @return 미확인 신규 지원자 수
     */
    @Query("SELECT COUNT(a) FROM ApplicationEntity a " +
            "WHERE a.targetSource = :source " +
            "AND a.targetPostId IN :postIds " +
            "AND a.status = net.kumo.kumo.domain.entity.Enum.ApplicationStatus.APPLIED")
    long countUnreadBySourceAndPostIds(
            @Param("source") String source,
            @Param("postIds") List<Long> postIds);

    /**
     * 특정 구직자와 연관된 모든 지원 내역을 삭제합니다. (회원 탈퇴 시 사용)
     *
     * @param user 삭제할 구직자 엔티티
     */
    void deleteBySeeker(UserEntity user);
}