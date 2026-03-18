package net.kumo.kumo.repository;

import net.kumo.kumo.domain.entity.NotificationEntity;
import net.kumo.kumo.domain.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 시스템 알림(Notification) 내역에 대한 데이터베이스 접근을 담당하는 리포지토리 인터페이스입니다.
 */
@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

    /**
     * 특정 사용자의 전체 알림 목록을 생성 일시 기준 내림차순(최신순)으로 조회합니다.
     *
     * @param userId 조회를 요청한 사용자의 식별자
     * @return 최신순으로 정렬된 알림 엔티티 리스트
     */
    List<NotificationEntity> findByUser_UserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 특정 사용자의 알림 중 아직 열람하지 않은(isRead = false) 알림의 총 개수를 계산합니다.
     *
     * @param userId 조회를 요청한 사용자의 식별자
     * @return 미열람 알림 개수
     */
    long countByUser_UserIdAndIsReadFalse(Long userId);

    /**
     * 특정 사용자의 알림 중 아직 열람하지 않은 알림 목록만을 조회합니다.
     *
     * @param userId 조회를 요청한 사용자의 식별자
     * @return 미열람 알림 엔티티 리스트
     */
    List<NotificationEntity> findByUser_UserIdAndIsReadFalse(Long userId);

    /**
     * 특정 사용자가 수신한 모든 알림의 상태를 일괄 '읽음(true)'으로 갱신합니다.
     *
     * @param userId 상태를 갱신할 대상 사용자의 식별자
     */
    @Modifying
    @Query("UPDATE NotificationEntity n SET n.isRead = true WHERE n.user.userId = :userId")
    void markAllAsRead(@Param("userId") Long userId);

    /**
     * 특정 사용자가 수신한 모든 알림 내역을 데이터베이스에서 일괄 삭제(Bulk Delete)합니다.
     *
     * @param userId 삭제할 대상 사용자의 식별자
     */
    @Modifying
    @Query("DELETE FROM NotificationEntity n WHERE n.user.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    /**
     * 특정 사용자가 수신한 모든 알림 내역을 엔티티 기반으로 삭제합니다. (회원 탈퇴 시 사용)
     *
     * @param user 삭제할 대상 사용자 엔티티
     */
    void deleteByUser(UserEntity user);
}