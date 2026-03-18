package net.kumo.kumo.repository;

import net.kumo.kumo.domain.entity.CompanyImageEntity;
import net.kumo.kumo.domain.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 구인자의 회사(사업장) 이미지 정보에 대한 데이터베이스 접근을 담당하는 리포지토리 인터페이스입니다.
 */
public interface CompanyImageRepository extends JpaRepository<CompanyImageEntity, Long> {

    /**
     * 특정 사용자(구인자)가 등록한 모든 회사 이미지 내역을 데이터베이스에서 삭제합니다. (회원 탈퇴 시 사용)
     *
     * @param user 삭제할 대상 사용자 엔티티
     */
    void deleteByUser(UserEntity user);
}