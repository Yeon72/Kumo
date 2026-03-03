package net.kumo.kumo.repository;

import net.kumo.kumo.domain.entity.SeekerCertificateEntity;
import net.kumo.kumo.domain.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface SeekerCertificateRepository extends JpaRepository<SeekerCertificateEntity, Long> {
	List<SeekerCertificateEntity> findByUser_UserId(Long userId);

	@Modifying
	@Transactional
	void deleteByUser(UserEntity user);
}