package net.kumo.kumo.repository;

import net.kumo.kumo.domain.entity.NotificationEntity;
import net.kumo.kumo.domain.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {
	
	// 1. 특정 사용자의 알림 목록 최신순 조회
	List<NotificationEntity> findByUser_UserIdOrderByCreatedAtDesc(Long userId);
	
	// 2. 읽지 않은 알림 개수 카운트 (헤더 뱃지용)
	long countByUser_UserIdAndIsReadFalse(Long userId);
	
	// 3. 특정 유저의 모든 알림을 읽음 처리할 때 사용
	List<NotificationEntity> findByUser_UserIdAndIsReadFalse(Long userId);
	
	@Modifying
	@Query("UPDATE NotificationEntity n SET n.isRead = true WHERE n.user.userId = :userId")
	void markAllAsRead(@Param("userId") Long userId);
	
	// [추가] 특정 유저의 알림 전체 삭제 (Bulk Delete)
	@Modifying // INSERT, UPDATE, DELETE 쿼리 실행 시 필수
	@Query("DELETE FROM NotificationEntity n WHERE n.user.userId = :userId")
	void deleteAllByUserId(@Param("userId") Long userId);
	
	void deleteByUser(UserEntity user);
	
	// 안 읽은(isRead = false) 알림 개수만 세기
}