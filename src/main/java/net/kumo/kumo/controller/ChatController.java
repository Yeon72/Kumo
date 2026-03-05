package net.kumo.kumo.controller;

import lombok.RequiredArgsConstructor;
import net.kumo.kumo.domain.dto.ChatMessageDTO;
import net.kumo.kumo.domain.dto.ChatRoomListDTO;
import net.kumo.kumo.domain.entity.ChatRoomEntity;
import net.kumo.kumo.domain.entity.UserEntity;
import net.kumo.kumo.service.ChatService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import net.kumo.kumo.repository.UserRepository;

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
    private final SimpMessagingTemplate messagingTemplate;

    // ★ 1. DB에서 진짜 내 정보를 찾아올 무기 장착 완료!
    private final UserRepository userRepository;

    // [이식 포인트] application.properties의 file.upload.chat 값을 가져옵니다.
    @Value("${file.upload.chat}")
    private String chatUploadDir;

    // ======================================================================
    // 1. [화면 연결] 웹 페이지 이동 관련 (HTTP)
    // ======================================================================

    @GetMapping("/chat/room/{roomId}")
    public String enterRoom(@PathVariable("roomId") Long roomId,
            @RequestParam(value = "userId", required = false) Long userId,
            Model model) {

        if (userId == null) {
            return "redirect:/chat/list";
        }

        ChatRoomEntity room = chatService.getChatRoom(roomId);

        // ★ 바로 이 부분입니다! roomId 옆에 userId를 추가해서 Service로 던져줍니다.
        List<ChatMessageDTO> history = chatService.getMessageHistory(roomId, userId);

        UserEntity opponent;
        if (room.getSeeker().getUserId().equals(userId)) {
            opponent = room.getRecruiter();
        } else {
            opponent = room.getSeeker();
        }

        model.addAttribute("roomId", roomId);
        model.addAttribute("userId", userId);
        model.addAttribute("history", history);

        model.addAttribute("roomName", opponent.getNickname());
        model.addAttribute("jobTitle", room.getJobPosting().getTitle());
        model.addAttribute("salary", room.getJobPosting().getSalaryAmount());
        model.addAttribute("address", room.getJobPosting().getWorkAddress());

        return "chat/chat_room";
    }

    // ★★★ 여기가 도플갱어 퇴마 수술 포인트입니다! ★★★
    @GetMapping("/chat/list")
    public String chatList(Principal principal, Model model) {

        // 1. 로그인 안 했으면 로그인 창으로 쫓아냅니다.
        if (principal == null) {
            return "redirect:/login";
        }

        // 🌟 2. 순리대로 갑니다. 시큐리티에서 '진짜 현재 접속한 사람의 이메일'을 뽑아옵니다.
        String loginEmail = principal.getName();

        // 🌟 3. 뽑아온 이메일로 DB를 뒤져서 진짜 내 정보를 가져오고, 내 고유 번호(Long)를 꺼냅니다!
        UserEntity user = userRepository.findByEmail(loginEmail)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        // ★ 가짜 6번 박살! 이제 무조건 진짜 내 ID가 들어갑니다!
        Long myId = user.getUserId();

        // 4. 진짜 내 ID로 방 목록 가져오기
        List<ChatRoomListDTO> chatRooms = chatService.getChatRoomsForUser(myId);

        model.addAttribute("chatRooms", chatRooms);
        model.addAttribute("userId", myId);

        return "chat/chat_list";
    }

    // ======================================================================
    // 2. [기능 추가] 사진 업로드 API (Ajax 요청용)
    // ======================================================================
    @PostMapping("/chat/upload")
    @ResponseBody
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty())
                return ResponseEntity.badRequest().body("파일이 없습니다.");

            // [추가] 일본 시장 대응을 위한 확장자 및 용량 필터링 (기존 로직 보존을 위해 상단에 배치)
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null)
                return ResponseEntity.badRequest().body("파일명 오류");

            String ext = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
            // 이미지(기존) + 문서(추가) + 최신 웹 이미지 포맷(webp, avif) 허용 리스트
            List<String> allowedExts = Arrays.asList("jpg", "jpeg", "png", "gif", "webp", "avif", "pdf", "docx", "doc",
                    "xlsx", "xls", "txt");

            if (!allowedExts.contains(ext)) {
                return ResponseEntity.badRequest().body("업로드 실패: 지원하지 않는 형식입니다.");
            }

            // [추가] 일본 네트워크 환경 고려: 10MB 제한 (필요시 조절)
            if (file.getSize() > 10 * 1024 * 1024) {
                return ResponseEntity.badRequest().body("업로드 실패: 용량 초과 (최대 10MB)");
            }

            // ================== 여기서부터 기존 로직 (절대 건드리지 않음) ==================
            String rootPath = System.getProperty("user.dir");
            String fullPath = rootPath + "/" + chatUploadDir;

            String savedFilename = UUID.randomUUID().toString() + "_" + originalFilename;

            File folder = new File(fullPath);
            if (!folder.exists())
                folder.mkdirs();

            File dest = new File(fullPath + savedFilename);
            file.transferTo(dest);

            // WebMvcConfig 설정을 그대로 따름
            return ResponseEntity.ok("/chat_images/" + savedFilename);
            // ===========================================================================

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("업로드 실패");
        }
    }

    // ======================================================================
    // 3. [실시간 통신] 메시지 주고 받기 (WebSocket)
    // ======================================================================

    @MessageMapping("/chat/message")
    public void sendMessage(ChatMessageDTO messageDTO) {
        // 1. DB에 메시지 저장
        ChatMessageDTO savedMessage = chatService.saveMessage(messageDTO);

        // 2. 채팅방 '안'에 있는 사람들에게 쏘기 (기존)
        messagingTemplate.convertAndSend("/sub/chat/room/" + savedMessage.getRoomId(), savedMessage);

        // ==========================================================
        // ★ 3. 채팅방 '밖(목록)'에 있는 두 사람의 개인 채널로도 알림 쏘기! ★
        // ==========================================================
        try {
            // 방 정보를 가져와서 구직자와 구인자 ID를 알아냅니다.
            ChatRoomEntity room = chatService.getChatRoom(savedMessage.getRoomId());
            Long seekerId = room.getSeeker().getUserId();
            Long recruiterId = room.getRecruiter().getUserId();

            // 두 사람의 전용 로비(Lobby) 파이프로 방금 저장된 메시지를 똑같이 배달합니다!
            messagingTemplate.convertAndSend("/sub/chat/user/" + seekerId, savedMessage);
            messagingTemplate.convertAndSend("/sub/chat/user/" + recruiterId, savedMessage);

        } catch (Exception e) {
            System.out.println("🚨 목록 실시간 갱신용 알림 발송 실패: " + e.getMessage());
        }
    }

    @GetMapping("/chat/create")
    public String createRoom(
            @RequestParam("recruiterId") Long recruiterId,
            @RequestParam(value = "jobPostId", required = false) Long jobPostId,
            @RequestParam("userId") Long seekerId) { // 현재 로그인한 구직자 ID

        // 1. 서비스에서 방을 찾거나 생성
        ChatRoomEntity room = chatService.createOrGetChatRoom(seekerId, recruiterId, jobPostId);

        // 2. 생성된(혹은 찾은) 방으로 즉시 이동
        return "redirect:/chat/room/" + room.getId() + "?userId=" + seekerId;
    }

    // ======================================================================
    // ★ [LIVE] 상대방이 방에 들어오거나 메시지를 읽었을 때 '1' 지우기 ★
    // ======================================================================
    @MessageMapping("/chat/read")
    public void processRead(ChatMessageDTO readSignal) {
        // 1. DB 업데이트: 읽음 신호를 보낸 사람이 밀린 메시지를 다 읽었다고 DB에 반영
        chatService.processLiveReadSignal(readSignal.getRoomId(), readSignal.getSenderId());

        // 2. 채팅방 안에 있는 두 사람에게 "방금 다 읽었대! 1 지워!" 하고 브로드캐스팅
        messagingTemplate.convertAndSend("/sub/chat/room/" + readSignal.getRoomId(), readSignal);
    }

    // ★ 프론트(JS)에서 안 읽은 개수를 가져갈 수 있게 API 주소 개방! (현우님 프로젝트 맞춤형)
    @GetMapping("/api/chat/unread-count")
    @ResponseBody
    public ResponseEntity<Long> getUnreadCount(Principal principal) {
        // 1. 로그인 안 한 유저면 0개 리턴
        if (principal == null) {
            return ResponseEntity.ok(0L);
        }

        // 2. 현재 접속한 사람의 이메일을 뽑아옵니다 (chatList에 있던 그 방식 그대로!)
        String loginEmail = principal.getName();

        // 3. 이메일로 내 정보를 찾아서 PK(userId)를 꺼냅니다.
        Long userId = userRepository.findByEmail(loginEmail)
                .map(UserEntity::getUserId)
                .orElse(null);

        // 유저를 못 찾았으면 안전하게 0 리턴
        if (userId == null) {
            return ResponseEntity.ok(0L);
        }

        // 4. 서비스 시켜서 숫자 가져온 다음 화면으로 던져줌
        long unreadCount = chatService.getUnreadMessageCount(userId);
        return ResponseEntity.ok(unreadCount);
    }
}