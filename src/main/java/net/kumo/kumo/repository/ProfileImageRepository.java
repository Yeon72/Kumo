package net.kumo.kumo.repository;

import net.kumo.kumo.domain.entity.ProfileImageEntity;
import net.kumo.kumo.domain.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileImageRepository extends JpaRepository<ProfileImageEntity, Long> {
	void deleteByUser(UserEntity user);
}

