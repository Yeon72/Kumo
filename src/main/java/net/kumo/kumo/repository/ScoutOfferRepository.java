package net.kumo.kumo.repository;

import net.kumo.kumo.domain.entity.ScoutOfferEntity;
import net.kumo.kumo.domain.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * 구인자가 구직자에게 보낸 스카우트 제의(ScoutOffer) 내역에 대한 데이터베이스 접근을 담당하는 리포지토리 인터페이스입니다.
 */
public interface ScoutOfferRepository extends JpaRepository<ScoutOfferEntity, Long> {

    /**
     * 특정 구직자가 받은 스카우트 제의 목록을 생성 일시 기준 내림차순(최신순)으로 조회합니다.
     *
     * @param seeker 조회를 요청한 구직자 엔티티
     * @return 최신순으로 정렬된 스카우트 제의 엔티티 리스트
     */
    List<ScoutOfferEntity> findBySeekerOrderByCreatedAtDesc(UserEntity seeker);

    /**
     * 특정 구인자가 특정 구직자에게 보낸 스카우트 제의 중, 특정 상태(예: PENDING)인 제의가 존재하는지 확인합니다.
     * 중복 스카우트 발송을 방지하기 위해 사용됩니다.
     *
     * @param recruiter 발송한 구인자 엔티티
     * @param seeker    수신한 구직자 엔티티
     * @param status    확인할 스카우트 상태
     * @return 조건에 일치하는 제의 존재 여부
     */
    boolean existsByRecruiterAndSeekerAndStatus(UserEntity recruiter, UserEntity seeker, ScoutOfferEntity.ScoutStatus status);

    /**
     * 특정 구인자가 발송한 모든 스카우트 제의 내역을 삭제합니다. (회원 탈퇴 시 사용)
     *
     * @param user 삭제할 구인자 엔티티
     */
    void deleteByRecruiter(UserEntity user);

    /**
     * 특정 구직자가 수신한 모든 스카우트 제의 내역을 삭제합니다. (회원 탈퇴 시 사용)
     *
     * @param user 삭제할 구직자 엔티티
     */
    void deleteBySeeker(UserEntity user);
}