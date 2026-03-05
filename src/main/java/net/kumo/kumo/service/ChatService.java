package net.kumo.kumo.service;

import lombok.RequiredArgsConstructor;
import net.kumo.kumo.domain.dto.ChatMessageDTO;
import net.kumo.kumo.domain.dto.ChatRoomListDTO;
import net.kumo.kumo.domain.entity.ChatMessageEntity;
import net.kumo.kumo.domain.entity.ChatRoomEntity;
import net.kumo.kumo.domain.entity.Enum.MessageType;
import net.kumo.kumo.domain.entity.JobPostingEntity;
import net.kumo.kumo.domain.entity.UserEntity;
import net.kumo.kumo.repository.UserRepository;
import net.kumo.kumo.repository.ChatMessageRepository;
import net.kumo.kumo.repository.ChatRoomRepository;
import net.kumo.kumo.repository.JobPostingRepository; // ★ 1. 임포트 완벽 복구
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
        private final JobPostingRepository jobPostingRepository; // ★ 2. 주석 해제 완벽 복구

        // ==========================================================
        // ★ 봉인 해제 + 구인공고 조립 완료된 방 생성 로직 ★
        // ==========================================================
        @Transactional
        public ChatRoomEntity createOrGetChatRoom(Long seekerId, Long recruiterId, Long jobPostId) {

                Optional<ChatRoomEntity> existingRoom = chatRoomRepository.findBySeeker_UserIdAndRecruiter_UserId(
                                seekerId,
                                recruiterId);

                if (existingRoom.isPresent()) {
                        return existingRoom.get();
                } // ★ 3. 꼬였던 중괄호 복구 완료

                UserEntity seeker = userRepository.findById(seekerId)
                                .orElseThrow(() -> new IllegalArgumentException("구직자를 찾을 수 없습니다."));

                UserEntity recruiter = userRepository.findById(recruiterId)
                                .orElseThrow(() -> new IllegalArgumentException("구인자를 찾을 수 없습니다."));

                JobPostingEntity jobPosting = null;
                if (jobPostId != null) { // ★ 4. 날아갔던 if문 조건식 복구 완료
                        jobPosting = jobPostingRepository.findById(jobPostId)
                                        .orElseThrow(() -> new IllegalArgumentException("구인공고를 찾을 수 없습니다."));
                }

                ChatRoomEntity newRoom = ChatRoomEntity.builder()
                                .seeker(seeker)
                                .recruiter(recruiter)
                                .jobPosting(jobPosting)
                                .build();

                return chatRoomRepository.save(newRoom);
        }

        // ==========================================================
        // ★ 5. 지워졌던 기존 통신용/조회용 필수 메서드들 전면 복구 ★
        // ==========================================================

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

        public List<ChatMessageDTO> getMessageHistory(Long roomId, Long userId) {
                chatMessageRepository.markMessagesAsRead(roomId, userId);
                return chatMessageRepository.findByRoom_IdOrderByCreatedAtAsc(roomId)
                                .stream()
                                .map(this::convertToDTO)
                                .collect(Collectors.toList());
        }

        public ChatRoomEntity getChatRoom(Long roomId) {
                return chatRoomRepository.findById(roomId)
                                .orElseThrow(() -> new IllegalArgumentException("방이 없습니다."));
        }

        private ChatMessageDTO convertToDTO(ChatMessageEntity entity) {
                return ChatMessageDTO.builder()
                                .roomId(entity.getRoom().getId())
                                .senderId(entity.getSender() != null ? entity.getSender().getUserId() : null)
                                .senderNickname(entity.getSender() != null ? entity.getSender().getNickname()
                                                : "알 수 없음")
                                .content(entity.getContent())
                                .messageType(entity.getMessageType().name())
                                .createdAt(entity.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm")))
                                .createdDate(entity.getCreatedAt().format(
                                                java.time.format.DateTimeFormatter.ofPattern("yyyy년 M월 d일 EEEE",
                                                                java.util.Locale.KOREAN)))
                                .isRead(entity.getIsRead())
                                .build();
        }

        @Transactional(readOnly = true)
        public List<ChatRoomListDTO> getChatRoomsForUser(Long userId) {
                List<ChatRoomEntity> rooms = chatRoomRepository.findBySeekerUserIdOrRecruiterUserId(userId, userId);

                return rooms.stream().map(room -> {
                        UserEntity opponent = room.getSeeker().getUserId().equals(userId) ? room.getRecruiter()
                                        : room.getSeeker();
                        ChatMessageEntity lastMsg = chatMessageRepository.findFirstByRoomOrderByCreatedAtDesc(room);
                        boolean hasUnreadFlag = chatMessageRepository
                                        .existsByRoomAndSender_UserIdNotAndIsReadFalse(room, userId);

                        return ChatRoomListDTO.builder()
                                        .roomId(room.getId())
                                        .opponentNickname(opponent.getNickname())
                                        .lastMessage(lastMsg != null ? lastMsg.getContent() : "대화 내용이 없습니다.")
                                        .lastTime(
                                                        lastMsg != null ? lastMsg.getCreatedAt().format(
                                                                        DateTimeFormatter.ofPattern("HH:mm")) : "")
                                        .hasUnread(hasUnreadFlag)
                                        .build();
                }).collect(Collectors.toList());
        }

        @Transactional
        public void processLiveReadSignal(Long roomId, Long readerId) {
                chatMessageRepository.markMessagesAsRead(roomId, readerId);
        }

        // ★ 안 읽은 메시지 총 개수 조회 서비스
        @Transactional(readOnly = true)
        public long getUnreadMessageCount(Long userId) {
                if (userId == null) {
                        return 0;
                }
                // 아까 ChatRoomRepository에 만든 쿼리를 여기서 부릅니다!
                return chatRoomRepository.countUnreadMessages(userId);
        }
}
