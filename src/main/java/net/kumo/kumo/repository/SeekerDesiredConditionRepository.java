package net.kumo.kumo.repository;

import net.kumo.kumo.domain.entity.SeekerDesiredConditionEntity;
import net.kumo.kumo.domain.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 구직자가 희망하는 근무 조건(Desired Condition) 정보에 대한
 * 데이터베이스 접근을 담당하는 리포지토리 인터페이스입니다.
 */
@Repository
public interface SeekerDesiredConditionRepository extends JpaRepository<SeekerDesiredConditionEntity, Long> {

    /**
     * 특정 사용자 식별자를 기반으로 구직자의 희망 근무 조건을 조회합니다.
     * (1:1 관계이므로 Optional로 반환합니다.)
     *
     * @param userId 조회를 요청한 사용자 식별자
     * @return 해당 사용자의 희망 근무 조건 엔티티 (존재하지 않을 경우 Optional.empty 반환)
     */
    Optional<SeekerDesiredConditionEntity> findByUser_UserId(Long userId);

    /**
     * 특정 사용자의 희망 근무 조건 내역을 데이터베이스에서 삭제합니다. (회원 탈퇴 시 사용)
     *
     * @param user 삭제할 대상 사용자 엔티티
     */
    @Modifying
    @Transactional
    void deleteByUser(UserEntity user);
}