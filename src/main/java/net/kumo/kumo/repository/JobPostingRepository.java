package net.kumo.kumo.repository;

import net.kumo.kumo.domain.entity.JobPostingEntity;
import net.kumo.kumo.domain.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 시스템 내부에 직접 등록된 구인 공고(JobPosting)에 대한 데이터베이스 접근을 담당하는 리포지토리 인터페이스입니다.
 */
@Repository
public interface JobPostingRepository extends JpaRepository<JobPostingEntity, Long> {

    /**
     * 전체 구인 공고 목록을 등록 일시(생성일) 기준 내림차순(최신순)으로 조회합니다.
     *
     * @return 최신순으로 정렬된 구인 공고 엔티티 리스트
     */
    List<JobPostingEntity> findAllByOrderByCreatedAtDesc();

    /**
     * 특정 사용자가 등록한 모든 구인 공고 내역을 데이터베이스에서 삭제합니다. (회원 탈퇴 시 사용)
     *
     * @param user 삭제할 대상 작성자(구인자) 엔티티
     */
    void deleteByUser(UserEntity user);
}