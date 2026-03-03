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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

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

    // ChatController.java

    @GetMapping("/chat/list")
    public String chatList(
            @RequestParam(value = "userId", required = false) Long userId,
            Model model) {

        // 1. 방어 코드: userId가 없으면 로그인 페이지로 리다이렉트
        if (userId == null) {
            return "redirect:/login";
        }

        // 2. 서비스 호출: 최신 메시지와 시간이 포함된 DTO 리스트 가져오기
        // (메서드 명은 아까 수정한 getChatRoomsForUser 입니다)
        List<ChatRoomListDTO> chatRooms = chatService.getChatRoomsForUser(userId);

        // 더미데이터
        // 2. ★ 가라(Dummy) 데이터 2개 강제 주입
        // ABC カンパニー 추가
        chatRooms.add(ChatRoomListDTO.builder()
                .roomId(999L) // 가짜 ID
                .opponentNickname("ABC カンパニー")
                .lastMessage("하나 궁금한게 있습니다")
                .lastTime("15:40")
                .build());

        // 오사카 한식당 추가
        chatRooms.add(ChatRoomListDTO.builder()
                .roomId(888L) // 가짜 ID
                .opponentNickname("오사카 한식당")
                .lastMessage("신청해주셔서 감사합니다. 유감이지만...")
                .lastTime("12:20")
                .build());

        // 3. 모델에 담아서 HTML로 전달
        model.addAttribute("chatRooms", chatRooms);
        model.addAttribute("userId", userId);

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
        ChatMessageDTO savedMessage = chatService.saveMessage(messageDTO);
        messagingTemplate.convertAndSend("/sub/chat/room/" + savedMessage.getRoomId(), savedMessage);
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
}