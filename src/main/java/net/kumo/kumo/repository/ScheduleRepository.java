package net.kumo.kumo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import net.kumo.kumo.domain.entity.ScheduleEntity;
import net.kumo.kumo.domain.entity.UserEntity;


public interface ScheduleRepository extends JpaRepository<ScheduleEntity, Long> {
    // 특정 유저(리크루터)의 일정만 싹 가져오는 마법의 메서드
    List<ScheduleEntity> findByUser(UserEntity user);
	
	void deleteByUser(UserEntity user);
}