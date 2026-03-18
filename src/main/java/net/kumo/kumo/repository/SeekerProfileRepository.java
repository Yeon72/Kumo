package net.kumo.kumo.repository;

import net.kumo.kumo.domain.entity.SeekerProfileEntity;
import net.kumo.kumo.domain.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 구직자의 이력서 프로필 설정 및 자기소개(Self PR) 정보에 대한
 * 데이터베이스 접근을 담당하는 리포지토리 인터페이스입니다.
 */
@Repository
public interface SeekerProfileRepository extends JpaRepository<SeekerProfileEntity, Long> {

    /**
     * 특정 사용자 식별자를 기반으로 구직자의 프로필 설정 정보를 조회합니다.
     *
     * @param userId 조회를 요청한 사용자 식별자
     * @return 해당 사용자의 프로필 엔티티 (존재하지 않을 경우 Optional.empty 반환)
     */
    Optional<SeekerProfileEntity> findByUser_UserId(Long userId);

    /**
     * 스카우트 제의 수신에 동의(scoutAgree = true)하고,
     * 이력서를 공개(isPublic = true)로 설정한 모든 구직자의 프로필 목록을 조회합니다.
     * (구인자의 인재 탐색 페이지 렌더링용)
     *
     * @return 조건에 부합하는 구직자 프로필 엔티티 리스트
     */
    List<SeekerProfileEntity> findByScoutAgreeTrueAndIsPublicTrue();

    /**
     * 특정 사용자의 프로필 설정 내역을 데이터베이스에서 삭제합니다. (회원 탈퇴 시 사용)
     *
     * @param user 삭제할 대상 사용자 엔티티
     */
    @Modifying
    @Transactional
    void deleteByUser(UserEntity user);
}