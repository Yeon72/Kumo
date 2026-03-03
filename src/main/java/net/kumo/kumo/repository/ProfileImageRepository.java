package net.kumo.kumo.repository;

import net.kumo.kumo.domain.entity.ProfileImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileImageRepository extends JpaRepository<ProfileImageEntity, Long> {
}

