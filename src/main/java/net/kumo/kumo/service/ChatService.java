package net.kumo.kumo.service;

import lombok.RequiredArgsConstructor;
import net.kumo.kumo.domain.dto.ChatMessageDTO;
import net.kumo.kumo.domain.dto.ChatRoomListDTO;
import net.kumo.kumo.domain.entity.ChatMessageEntity;
import net.kumo.kumo.domain.entity.ChatRoomEntity;
import net.kumo.kumo.domain.entity.Enum.MessageType;
import net.kumo.kumo.domain.entity.UserEntity;
import net.kumo.kumo.repository.ChatMessageRepository;
import net.kumo.kumo.repository.ChatRoomRepository;
import net.kumo.kumo.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    // ======================================================================
    // 🌟 1. 방 생성 및 조회 (오사카/도쿄 분리 완벽 대응)
    // ======================================================================
    public ChatRoomEntity createOrGetChatRoom(Long seekerId, Long recruiterId, Long targetPostId, String targetSource) {

        Optional<ChatRoomEntity> existingRoom = chatRoomRepository
                .findBySeeker_UserIdAndRecruiter_UserIdAndTargetPostIdAndTargetSource(
                        seekerId, recruiterId, targetPostId, targetSource);

        if (existingRoom.isPresent()) {
            return existingRoom.get();
        }

        UserEntity seeker = userRepository.findById(seekerId)
                .orElseThrow(() -> new IllegalArgumentException("구직자를 찾을 수 없습니다."));

        UserEntity recruiter = userRepository.findById(recruiterId)
                .orElseThrow(() -> new IllegalArgumentException("구인자를 찾을 수 없습니다."));

        ChatRoomEntity newRoom = ChatRoomEntity.builder()
                .seeker(seeker)
                .recruiter(recruiter)
                .targetPostId(targetPostId)
                .targetSource(targetSource)
                .build();

        return chatRoomRepository.save(newRoom);
    }

    // ======================================================================
    // 🌟 2. 단일 채팅방 정보 가져오기
    // ======================================================================
    @Transactional(readOnly = true)
    public ChatRoomEntity getChatRoom(Long roomId) {
        return chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("방이 없습니다."));
    }

    // ======================================================================
    // 🌟 3. 메시지 저장 (DTO 기반)
    // ======================================================================
    public ChatMessageDTO saveMessage(ChatMessageDTO dto) {
        ChatRoomEntity room = getChatRoom(dto.getRoomId());

        UserEntity sender = userRepository.findById(dto.getSenderId())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        ChatMessageEntity entity = ChatMessageEntity.builder()
                .room(room)
                .sender(sender)
                .content(dto.getContent())
                .messageType(MessageType.valueOf(dto.getMessageType()))
                .isRead(false)
                .build();

        ChatMessageEntity saved = chatMessageRepository.save(entity);
        return convertToDTO(saved);
    }

    // ======================================================================
    // 🌟 4. 대화 기록 가져오기 및 읽음 처리
    // ======================================================================
    public List<ChatMessageDTO> getMessageHistory(Long roomId, Long userId) {
        chatMessageRepository.markMessagesAsRead(roomId, userId);

        return chatMessageRepository.findByRoom_IdOrderByCreatedAtAsc(roomId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // ======================================================================
// 🌟 5. 내 채팅방 목록 가져오기 (상대방 프로필 이미지 로직 추가)
// ======================================================================
    @Transactional(readOnly = true)
    public List<ChatRoomListDTO> getChatRoomsForUser(Long userId) {

        List<ChatRoomEntity> rooms = chatRoomRepository.findChatRoomsByUserId(userId);

        return rooms.stream().map(room -> {
            UserEntity opponent = room.getSeeker().getUserId().equals(userId) ? room.getRecruiter() : room.getSeeker();

            // 🌟 [추가] 상대방 프로필 이미지 경로 추출 (기본값 설정)
            String profileImg = "/images/common/default_profile.png";
            if (opponent.getProfileImage() != null && opponent.getProfileImage().getFileUrl() != null) {
                profileImg = opponent.getProfileImage().getFileUrl();
            }

            ChatMessageEntity lastMsg = chatMessageRepository.findFirstByRoomOrderByCreatedAtDesc(room);
            boolean hasUnreadFlag = chatMessageRepository.existsByRoomAndSender_UserIdNotAndIsReadFalse(room, userId);

            return ChatRoomListDTO.builder()
                    .roomId(room.getId())
                    .opponentNickname(opponent.getNickname())
                    // 🌟 이 줄을 추가해야 목록에서 강아지가 사라집니다!
                    .opponentProfileImg(opponent.getProfileImage() != null ? opponent.getProfileImage().getFileUrl() : "/images/common/default_profile.png")
                    .lastMessage(lastMsg != null ? lastMsg.getContent() : "대화 내용이 없습니다.")
                    .lastTime(lastMsg != null ? lastMsg.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm")) : "")
                    .hasUnread(hasUnreadFlag)
                    .build();
        }).collect(Collectors.toList());
    }

    // ======================================================================
    // 🌟 6. 실시간 웹소켓 읽음 처리
    // ======================================================================
    public void processLiveReadSignal(Long roomId, Long readerId) {
        chatMessageRepository.markMessagesAsRead(roomId, readerId);
    }

    // 읽지 않은 메시지 개수 구함
    @Transactional(readOnly = true)
    public int getUnreadMessageCount(Long userId) {
        return chatMessageRepository.countUnreadMessagesForUser(userId);
    }

    // ======================================================================
    // 🌟 7. Entity -> DTO 변환 헬퍼 메서드
    // ======================================================================
    private ChatMessageDTO convertToDTO(ChatMessageEntity entity) {
        return ChatMessageDTO.builder()
                .roomId(entity.getRoom().getId())
                .senderId(entity.getSender() != null ? entity.getSender().getUserId() : null)
                .senderNickname(entity.getSender() != null ? entity.getSender().getNickname() : "알 수 없음")
                .content(entity.getContent())
                .messageType(entity.getMessageType().name())
                .createdAt(entity.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm")))
                .createdDate(entity.getCreatedAt().format(
                        java.time.format.DateTimeFormatter.ofPattern("yyyy년 M월 d일 EEEE", java.util.Locale.KOREAN)))
                .isRead(entity.getIsRead())
                .build();
    }
}