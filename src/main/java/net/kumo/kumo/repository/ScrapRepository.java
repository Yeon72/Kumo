package net.kumo.kumo.repository;

import net.kumo.kumo.domain.entity.ScrapEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScrapRepository extends JpaRepository<ScrapEntity, Long> {
	// 뒤에 AndSource 를 붙여줍니다!
	boolean existsByUserIdAndJobPostIdAndSource(Long userId, Long jobPostId, String source);
	void deleteByUserIdAndJobPostIdAndSource(Long userId, Long jobPostId, String source);
	
	// 🌟 [추가] 특정 유저의 찜 목록을 최신순으로 가져오기
	List<ScrapEntity> findByUserIdOrderByCreatedAtDesc(Long userId);
	
	void deleteByUserId(Long userId);
}