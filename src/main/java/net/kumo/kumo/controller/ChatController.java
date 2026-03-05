package net.kumo.kumo.controller;

import lombok.RequiredArgsConstructor;
import net.kumo.kumo.domain.dto.ChatMessageDTO;
import net.kumo.kumo.domain.dto.ChatRoomListDTO;
import net.kumo.kumo.domain.entity.ChatRoomEntity;
import net.kumo.kumo.domain.entity.UserEntity;
import net.kumo.kumo.repository.ChatRoomRepository;
import net.kumo.kumo.repository.UserRepository;
import net.kumo.kumo.service.ChatService;
import net.kumo.kumo.service.MapService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ChatRoomRepository chatRoomRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;
    private final MapService mapService;

    // [이식 포인트] application.properties의 file.upload.chat 값을 가져옵니다.
    @Value("${file.upload.chat}")
    private String chatUploadDir;

    // ======================================================================
    // 🌟 1. 방 생성 (오사카/도쿄 출처 연동 완벽 적용)
    // ======================================================================
    @GetMapping("/chat/create")
    public String createRoom(
            @RequestParam(value = "seekerId", required = false) Long targetSeekerId,
            @RequestParam(value = "recruiterId", required = false) Long targetRecruiterId,
            @RequestParam("jobPostId") Long jobPostId,
            @RequestParam("jobSource") String jobSource,
            @org.springframework.security.core.annotation.AuthenticationPrincipal net.kumo.kumo.security.AuthenticatedUser authUser) {

        UserEntity currentUser = userRepository.findByEmail(authUser.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("로그인 정보를 찾을 수 없습니다."));
        Long myId = currentUser.getUserId();

        Long finalSeekerId;
        Long finalRecruiterId;

        if ("RECRUITER".equals(currentUser.getRole().name())) {
            finalRecruiterId = myId;
            finalSeekerId = targetSeekerId;
        } else {
            finalSeekerId = myId;
            finalRecruiterId = targetRecruiterId;
        }

        ChatRoomEntity room = chatService.createOrGetChatRoom(finalSeekerId, finalRecruiterId, jobPostId, jobSource);

        return "redirect:/chat/room/" + room.getId() + "?userId=" + myId;
    }

    // ======================================================================
    // 🌟 2. 방 입장 (공고 상세정보 매핑 완벽 적용)
    // ======================================================================
    @GetMapping("/chat/room/{roomId}")
    public String enterRoom(@PathVariable Long roomId,
                            @RequestParam("userId") Long userId,
                            Model model) {

        ChatRoomEntity room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));

        UserEntity opponent = room.getSeeker().getUserId().equals(userId) ? room.getRecruiter() : room.getSeeker();
        model.addAttribute("roomName", opponent.getNickname());

        // =========================================================
        // 🌟 수정된 부분: 대화 기록 불러오기 & 모델에 담기 (읽음 처리도 자동 수행됨!)
        // =========================================================
        List<net.kumo.kumo.domain.dto.ChatMessageDTO> history = chatService.getMessageHistory(roomId, userId);
        model.addAttribute("chatHistory", history);
        // =========================================================

        try {
            net.kumo.kumo.domain.dto.JobDetailDTO jobDetail =
                    mapService.getJobDetail(room.getTargetPostId(), room.getTargetSource(), "ko");

            model.addAttribute("jobTitle", jobDetail.getTitle());
            model.addAttribute("salary", jobDetail.getWage());
            model.addAttribute("address", jobDetail.getAddress());
        } catch (Exception e) {
            model.addAttribute("jobTitle", "삭제되거나 마감된 공고입니다.");
            model.addAttribute("salary", "-");
            model.addAttribute("address", "-");
        }

        model.addAttribute("roomId", roomId);
        model.addAttribute("userId", userId);

        return "chat/chat_room";
    }

    // ======================================================================
    // 🌟 3. 채팅 목록 보기 (자동 유저 인식 기능 완벽 적용)
    // ======================================================================
    @GetMapping("/chat/list")
    public String chatList(
            @RequestParam(value = "userId", required = false) Long userId,
            @org.springframework.security.core.annotation.AuthenticationPrincipal net.kumo.kumo.security.AuthenticatedUser authUser,
            Model model) {

        // 🌟 1. URL에 userId가 넘어오지 않았다면? 현재 시큐리티 로그인 세션에서 아이디를 뽑아옵니다!
        if (userId == null && authUser != null) {
            UserEntity currentUser = userRepository.findByEmail(authUser.getEmail())
                    .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));
            userId = currentUser.getUserId();
        }

        // 🌟 2. 로그인도 안 되어 있고, 아이디도 없으면 튕겨냅니다.
        if (userId == null) {
            return "redirect:/login";
        }

        // 🌟 3. 더미 데이터 없이 실제 내 채팅방 목록만 가져오기
        List<ChatRoomListDTO> chatRooms = chatService.getChatRoomsForUser(userId);

        model.addAttribute("chatRooms", chatRooms);
        model.addAttribute("userId", userId); // HTML로 넘겨줘서 계속 쓸 수 있게 함

        return "chat/chat_list";
    }

    // ======================================================================
    // 🌟 4. 사진 업로드 API (Ajax)
    // ======================================================================
    @PostMapping("/chat/upload")
    @ResponseBody
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) return ResponseEntity.badRequest().body("파일이 없습니다.");

            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) return ResponseEntity.badRequest().body("파일명 오류");

            String ext = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
            List<String> allowedExts = Arrays.asList("jpg", "jpeg", "png", "gif", "webp", "avif", "pdf", "docx", "doc", "xlsx", "xls", "txt");

            if (!allowedExts.contains(ext)) {
                return ResponseEntity.badRequest().body("업로드 실패: 지원하지 않는 형식입니다.");
            }

            if (file.getSize() > 10 * 1024 * 1024) {
                return ResponseEntity.badRequest().body("업로드 실패: 용량 초과 (최대 10MB)");
            }

            String rootPath = System.getProperty("user.dir");
            String fullPath = rootPath + "/" + chatUploadDir;
            String savedFilename = UUID.randomUUID().toString() + "_" + originalFilename;

            File folder = new File(fullPath);
            if (!folder.exists()) folder.mkdirs();

            File dest = new File(fullPath + savedFilename);
            file.transferTo(dest);

            return ResponseEntity.ok("/chat_images/" + savedFilename);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("업로드 실패");
        }
    }

    // ======================================================================
    // 🌟 5. 메시지 전송 (WebSocket)
    // ======================================================================
    @MessageMapping("/chat/message")
    public void sendMessage(ChatMessageDTO messageDTO) {
        ChatMessageDTO savedMessage = chatService.saveMessage(messageDTO);
        messagingTemplate.convertAndSend("/sub/chat/room/" + savedMessage.getRoomId(), savedMessage);

        try {
            ChatRoomEntity room = chatService.getChatRoom(savedMessage.getRoomId());
            Long seekerId = room.getSeeker().getUserId();
            Long recruiterId = room.getRecruiter().getUserId();

            messagingTemplate.convertAndSend("/sub/chat/user/" + seekerId, savedMessage);
            messagingTemplate.convertAndSend("/sub/chat/user/" + recruiterId, savedMessage);
        } catch (Exception e) {
            System.out.println("🚨 목록 실시간 갱신용 알림 발송 실패: " + e.getMessage());
        }
    }

    // ======================================================================
    // 🌟 6. 읽음 처리 (WebSocket)
    // ======================================================================
    @MessageMapping("/chat/read")
    public void processRead(ChatMessageDTO readSignal) {
        chatService.processLiveReadSignal(readSignal.getRoomId(), readSignal.getSenderId());
        messagingTemplate.convertAndSend("/sub/chat/room/" + readSignal.getRoomId(), readSignal);
    }

    // 읽지 않은 메시지 개수 반환
    @GetMapping("/api/chat/unread-count")
    @ResponseBody // 화면 이동이 아니라 숫자 데이터만 반환하도록 설정
    public ResponseEntity<Integer> getUnreadCount(
            @org.springframework.security.core.annotation.AuthenticationPrincipal net.kumo.kumo.security.AuthenticatedUser authUser) {

        if (authUser == null) {
            return ResponseEntity.status(401).build(); // 로그인 안됨
        }

        UserEntity currentUser = userRepository.findByEmail(authUser.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

        int unreadCount = chatService.getUnreadMessageCount(currentUser.getUserId());

        return ResponseEntity.ok(unreadCount); // 안 읽은 개수 리턴
    }
}