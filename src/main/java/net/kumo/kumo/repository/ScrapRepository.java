package net.kumo.kumo.repository;

import net.kumo.kumo.domain.entity.ScrapEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 사용자가 즐겨찾기(스크랩)한 구인 공고 내역에 대한 데이터베이스 접근을 담당하는 리포지토리 인터페이스입니다.
 */
public interface ScrapRepository extends JpaRepository<ScrapEntity, Long> {

    /**
     * 특정 사용자가 특정 공고를 이미 스크랩(찜)했는지 여부를 확인합니다.
     *
     * @param userId    확인할 사용자 식별자
     * @param jobPostId 대상 공고 식별자
     * @param source    대상 공고의 데이터 출처 (OSAKA, TOKYO 등)
     * @return 스크랩 존재 여부
     */
    boolean existsByUserIdAndJobPostIdAndSource(Long userId, Long jobPostId, String source);

    /**
     * 특정 사용자가 스크랩한 특정 공고 내역을 데이터베이스에서 삭제(스크랩 취소)합니다.
     *
     * @param userId    삭제할 대상 사용자 식별자
     * @param jobPostId 삭제할 대상 공고 식별자
     * @param source    삭제할 대상 공고의 데이터 출처
     */
    void deleteByUserIdAndJobPostIdAndSource(Long userId, Long jobPostId, String source);

    /**
     * 특정 사용자의 전체 스크랩(찜) 목록을 최신순으로 조회합니다.
     *
     * @param userId 조회를 요청한 사용자 식별자
     * @return 최신순으로 정렬된 스크랩 엔티티 리스트
     */
    List<ScrapEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 특정 사용자의 전체 스크랩 내역을 데이터베이스에서 삭제합니다. (회원 탈퇴 시 사용)
     *
     * @param userId 삭제할 대상 사용자 식별자
     */
    void deleteByUserId(Long userId);
}