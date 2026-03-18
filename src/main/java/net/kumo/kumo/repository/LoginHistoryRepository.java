package net.kumo.kumo.repository;

import net.kumo.kumo.domain.entity.LoginHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 사용자 접속 이력 및 보안 로그(LoginHistory)에 대한 데이터베이스 접근을 담당하는 리포지토리 인터페이스입니다.
 */
public interface LoginHistoryRepository extends JpaRepository<LoginHistoryEntity, Long> {

    /**
     * 특정 이메일 계정과 연관된 모든 접속 시도 이력을 데이터베이스에서 삭제합니다. (회원 탈퇴 시 사용)
     *
     * @param email 삭제할 대상 사용자의 이메일 계정
     */
    void deleteByEmail(String email);
}