package net.kumo.kumo.repository;

import net.kumo.kumo.domain.entity.LoginHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginHistoryRepository extends JpaRepository<LoginHistoryEntity, Long> {
	void deleteByEmail(String email);
}
