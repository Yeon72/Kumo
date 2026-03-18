package net.kumo.kumo.repository;

import net.kumo.kumo.domain.entity.ChatMessageEntity;
import net.kumo.kumo.domain.entity.ChatRoomEntity;
import net.kumo.kumo.domain.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 채팅 메시지 내역(ChatMessage)에 대한 데이터베이스 접근을 담당하는 리포지토리 인터페이스입니다.
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {

    /**
     * 특정 채팅방 내부의 전체 대화 기록을 과거순(생성일시 오름차순)으로 조회합니다.
     *
     * @param roomId 조회할 채팅방 식별자
     * @return 해당 채팅방의 메시지 리스트
     */
    List<ChatMessageEntity> findByRoom_IdOrderByCreatedAtAsc(Long roomId);

    /**
     * 채팅 목록 렌더링을 위해 특정 채팅방의 가장 최근 메시지 1건을 조회합니다.
     *
     * @param room 조회할 채팅방 엔티티
     * @return 가장 최근에 작성된 메시지 엔티티
     */
    ChatMessageEntity findFirstByRoomOrderByCreatedAtDesc(ChatRoomEntity room);

    /**
     * 특정 채팅방 내에 본인이 발송하지 않았으며, 아직 읽지 않은 메시지가 존재하는지 확인합니다.
     *
     * @param room   확인할 채팅방 엔티티
     * @param userId 본인의 식별자
     * @return 미확인 메시지 존재 여부
     */
    boolean existsByRoomAndSender_UserIdNotAndIsReadFalse(ChatRoomEntity room, Long userId);

    /**
     * 사용자가 채팅방에 입장했을 때, 상대방이 발송한 미확인 메시지를 일괄 '읽음(true)' 처리합니다.
     *
     * @param roomId 읽음 처리할 채팅방 식별자
     * @param userId 입장한 사용자(본인) 식별자
     */
    @Modifying
    @Query("UPDATE ChatMessageEntity m SET m.isRead = true WHERE m.room.id = :roomId AND m.sender.userId != :userId AND m.isRead = false")
    void markMessagesAsRead(@Param("roomId") Long roomId, @Param("userId") Long userId);

    /**
     * 특정 사용자가 참여 중인 모든 채팅방을 통틀어 아직 읽지 않은 총 메시지 개수를 계산합니다.
     *
     * @param userId 대상 사용자 식별자
     * @return 미확인 메시지의 총 개수
     */
    @Query("SELECT COUNT(m) FROM ChatMessageEntity m " +
            "WHERE (m.room.seeker.userId = :userId OR m.room.recruiter.userId = :userId) " +
            "AND m.sender.userId != :userId " +
            "AND m.isRead = false")
    int countUnreadMessagesForUser(@Param("userId") Long userId);

    /**
     * 특정 사용자가 발송한 모든 채팅 메시지 내역을 삭제합니다. (회원 탈퇴 시 사용)
     *
     * @param user 삭제할 작성자 엔티티
     */
    void deleteBySender(UserEntity user);
}