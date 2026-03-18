package net.kumo.kumo.repository;

import net.kumo.kumo.domain.entity.ProfileImageEntity;
import net.kumo.kumo.domain.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 사용자 프로필 이미지 메타데이터 및 스토리지 경로에 대한
 * 데이터베이스 접근을 담당하는 리포지토리 인터페이스입니다.
 */
public interface ProfileImageRepository extends JpaRepository<ProfileImageEntity, Long> {

    /**
     * 특정 사용자가 등록한 프로필 이미지 내역을 데이터베이스에서 삭제합니다. (회원 탈퇴 시 사용)
     *
     * @param user 삭제할 대상 사용자 엔티티
     */
    void deleteByUser(UserEntity user);
}