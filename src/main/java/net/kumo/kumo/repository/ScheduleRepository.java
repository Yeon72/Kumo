package net.kumo.kumo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import net.kumo.kumo.domain.entity.ScheduleEntity;
import net.kumo.kumo.domain.entity.UserEntity;

/**
 * 사용자(구인자)의 캘린더 일정(Schedule) 관리에 대한 데이터베이스 접근을 담당하는 리포지토리 인터페이스입니다.
 */
public interface ScheduleRepository extends JpaRepository<ScheduleEntity, Long> {

    /**
     * 특정 사용자 계정에 등록된 모든 캘린더 일정 목록을 조회합니다.
     *
     * @param user 조회를 요청한 사용자 엔티티
     * @return 해당 사용자의 일정 엔티티 리스트
     */
    List<ScheduleEntity> findByUser(UserEntity user);

    /**
     * 특정 사용자 계정에 등록된 모든 캘린더 일정 내역을 데이터베이스에서 삭제합니다. (회원 탈퇴 시 사용)
     *
     * @param user 삭제할 대상 사용자 엔티티
     */
    void deleteByUser(UserEntity user);
}