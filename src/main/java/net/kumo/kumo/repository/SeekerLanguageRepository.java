package net.kumo.kumo.repository;

import net.kumo.kumo.domain.entity.SeekerLanguageEntity;
import net.kumo.kumo.domain.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 구직자의 구사 가능 언어 및 어학 능력(Language) 정보에 대한
 * 데이터베이스 접근을 담당하는 리포지토리 인터페이스입니다.
 */
@Repository
public interface SeekerLanguageRepository extends JpaRepository<SeekerLanguageEntity, Long> {

    /**
     * 특정 사용자 식별자를 기반으로 등록된 모든 어학 능력 목록을 조회합니다.
     *
     * @param userId 조회를 요청한 사용자 식별자
     * @return 해당 사용자의 어학 능력 엔티티 리스트
     */
    List<SeekerLanguageEntity> findByUser_UserId(Long userId);

    /**
     * 특정 사용자 식별자를 기반으로 등록된 전체 어학 능력 목록을 모두 조회합니다.
     *
     * @param userId 조회를 요청한 사용자 식별자
     * @return 해당 사용자의 어학 능력 엔티티 리스트
     */
    List<SeekerLanguageEntity> findAllByUser_UserId(Long userId);

    /**
     * 특정 사용자의 전체 어학 능력 내역을 데이터베이스에서 일괄 삭제합니다. (이력서 갱신 또는 회원 탈퇴 시 사용)
     *
     * @param user 삭제할 대상 사용자 엔티티
     */
    @Modifying
    @Transactional
    void deleteByUser(UserEntity user);
}