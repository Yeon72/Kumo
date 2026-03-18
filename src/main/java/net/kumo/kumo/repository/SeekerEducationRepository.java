package net.kumo.kumo.repository;

import net.kumo.kumo.domain.entity.SeekerEducationEntity;
import net.kumo.kumo.domain.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

/**
 * 구직자의 최종 학력 및 교육(Education) 이력에 대한
 * 데이터베이스 접근을 담당하는 리포지토리 인터페이스입니다.
 */
@Repository
public interface SeekerEducationRepository extends JpaRepository<SeekerEducationEntity, Long> {

    /**
     * 특정 사용자 식별자를 기반으로 학력 정보를 조회합니다.
     *
     * @param userId 조회를 요청한 사용자 식별자
     * @return 해당 사용자의 학력 엔티티
     */
    SeekerEducationEntity findByUser_UserId(Long userId);

    /**
     * 특정 사용자의 학력 내역을 데이터베이스에서 일괄 삭제합니다. (이력서 갱신 또는 회원 탈퇴 시 사용)
     *
     * @param user 삭제할 대상 사용자 엔티티
     */
    @Modifying
    @Transactional
    void deleteByUser(UserEntity user);
}