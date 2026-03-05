package net.kumo.kumo.repository;

import net.kumo.kumo.domain.entity.CompanyImageEntity;
import net.kumo.kumo.domain.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompanyImageRepository extends JpaRepository<CompanyImageEntity, Long> {
	void deleteByUser(UserEntity user);
}