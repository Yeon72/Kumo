package net.kumo.kumo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import net.kumo.kumo.domain.entity.CompanyEntity;
import net.kumo.kumo.domain.entity.UserEntity;


public interface CompanyRepository extends JpaRepository<CompanyEntity, Long> {
    // 특정 사용자가 등록한 회사 목록만 가져오기
    List<CompanyEntity> findAllByUser(UserEntity user);
	
	void deleteByUser(UserEntity user);
}