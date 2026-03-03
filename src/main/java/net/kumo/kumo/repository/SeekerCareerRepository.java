package net.kumo.kumo.repository;

import net.kumo.kumo.domain.entity.SeekerCareerEntity;
import net.kumo.kumo.domain.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface SeekerCareerRepository extends JpaRepository<SeekerCareerEntity, Long> {
	List<SeekerCareerEntity> findByUser_UserId(Long userId);

	@Modifying
	@Transactional
	void deleteByUser(UserEntity user);
}