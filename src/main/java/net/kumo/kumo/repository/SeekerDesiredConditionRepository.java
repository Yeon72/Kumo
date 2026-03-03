package net.kumo.kumo.repository;

import net.kumo.kumo.domain.entity.SeekerDesiredConditionEntity;
import net.kumo.kumo.domain.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface SeekerDesiredConditionRepository extends JpaRepository<SeekerDesiredConditionEntity, Long> {
	Optional<SeekerDesiredConditionEntity> findByUser_UserId(Long userId);

	@Modifying
	@Transactional
	void deleteByUser(UserEntity user);
}