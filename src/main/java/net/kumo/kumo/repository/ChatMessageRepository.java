package net.kumo.kumo.repository;

import net.kumo.kumo.domain.entity.ChatMessageEntity;
import net.kumo.kumo.domain.entity.ChatRoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query; // ★ 추가됨
import org.springframework.data.repository.query.Param; // ★ 추가됨
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {

    // 1. 전체 대화 기록 조회 (채팅방 진입 시)
    List<ChatMessageEntity> findByRoom_IdOrderByCreatedAtAsc(Long roomId);

    // 2. 최신 메시지 1건 조회 (채팅 목록 출력 시)
    ChatMessageEntity findFirstByRoomOrderByCreatedAtDesc(ChatRoomEntity room);

    // 3. 안 읽은 메시지가 있는지 확인 (채팅 목록 '읽지않음' 탭 필터용)
    boolean existsByRoomAndSender_UserIdNotAndIsReadFalse(ChatRoomEntity room, Long userId);

    // ★ 4. 신규 추가: 채팅방 입장 시 상대방의 메시지를 전부 '읽음(true/1)' 처리하는 업데이트 쿼리
    @Modifying
    @Query("UPDATE ChatMessageEntity m SET m.isRead = true WHERE m.room.id = :roomId AND m.sender.userId != :userId AND m.isRead = false")
    void markMessagesAsRead(@Param("roomId") Long roomId, @Param("userId") Long userId);
}