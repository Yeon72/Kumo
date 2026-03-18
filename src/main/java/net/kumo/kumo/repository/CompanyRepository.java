package net.kumo.kumo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import net.kumo.kumo.domain.entity.CompanyEntity;
import net.kumo.kumo.domain.entity.UserEntity;

/**
 * 구인자가 등록한 사업장(회사) 정보에 대한 데이터베이스 접근을 담당하는 리포지토리 인터페이스입니다.
 */
public interface CompanyRepository extends JpaRepository<CompanyEntity, Long> {

    /**
     * 특정 사용자(구인자) 계정에 등록된 모든 회사 목록을 조회합니다.
     *
     * @param user 조회를 요청한 사용자 엔티티
     * @return 해당 사용자가 등록한 회사 엔티티 리스트
     */
    List<CompanyEntity> findAllByUser(UserEntity user);

    /**
     * 특정 사용자(구인자) 계정에 등록된 모든 회사 정보를 데이터베이스에서 삭제합니다. (회원 탈퇴 시 사용)
     *
     * @param user 삭제할 대상 사용자 엔티티
     */
    void deleteByUser(UserEntity user);
}