package net.kumo.kumo.repository;

import net.kumo.kumo.domain.entity.JobPostingEntity;
import net.kumo.kumo.domain.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobPostingRepository extends JpaRepository<JobPostingEntity, Long> {
	// 최신순 조회
	List<JobPostingEntity> findAllByOrderByCreatedAtDesc();
	
	void deleteByUser(UserEntity user);
}