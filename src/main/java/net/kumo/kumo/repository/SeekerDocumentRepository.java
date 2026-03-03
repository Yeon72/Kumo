package net.kumo.kumo.repository;


import net.kumo.kumo.domain.entity.SeekerDocumentEntity;
import net.kumo.kumo.domain.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface SeekerDocumentRepository extends JpaRepository<SeekerDocumentEntity, Long> {
	
	// 🌟 특정 유저의 증빙서류 리스트만 쏙 뽑아올 때 사용합니다.
	List<SeekerDocumentEntity> findByUser(UserEntity user);
	
	@Modifying
	@Transactional
	void deleteByUser(UserEntity user);
}
