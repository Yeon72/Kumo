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
    // 1. [화면 연결] 웹 페이지 이동 관련 (HTTP)
    // ======================================================================
    @GetMapping("/chat/room/{roomId}")
    public String enterRoom(@PathVariable Long roomId,
                            @RequestParam("userId") Long userId,
                            Model model) {

        ChatRoomEntity room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));

        // (선택) 이전 채팅 내역 가져오기 로직 유지
        // List<ChatMessageEntity> history = chatMessageRepository.findByChatRoom_IdOrderByCreatedAtAsc(roomId);
        // model.addAttribute("chatHistory", history);

        // 상대방 닉네임 설정
        UserEntity opponent = room.getSeeker().getUserId().equals(userId) ? room.getRecruiter() : room.getSeeker();
        model.addAttribute("roomName", opponent.getNickname());

        // 🌟 핵심: MapService를 써서 공고 세부정보를 깔끔하게 가져옵니다!
        try {
            // "ko" 또는 "ja" 등 언어 설정은 필요에 따라 세션에서 가져와도 됩니다. (임시로 "ko" 적용)
            net.kumo.kumo.domain.dto.JobDetailDTO jobDetail =
                    mapService.getJobDetail(room.getTargetPostId(), room.getTargetSource(), "ko");

            // HTML 상단에 뿌려줄 정보 세팅
            model.addAttribute("jobTitle", jobDetail.getTitle());
            model.addAttribute("salary", jobDetail.getWage()); // DTO에 맞게 getWage() 사용
            model.addAttribute("address", jobDetail.getAddress());
        } catch (Exception e) {
            // 만약 공고가 삭제되었거나 마감된 경우를 대비한 방어 코드! (앱이 터지지 않게 보호)
            model.addAttribute("jobTitle", "삭제되거나 마감된 공고입니다.");
            model.addAttribute("salary", "-");
            model.addAttribute("address", "-");
        }

        model.addAttribute("roomId", roomId);
        model.addAttribute("userId", userId);

        return "chat/chat_room"; // 채팅방 HTML 경로 (본인 프로젝트에 맞게 확인)
    }

    /*
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
    } */

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

    // ======================================================================
    // ★ [수정됨] 채팅방 개설 및 입장 로직 (구인자 빙의 버그 해결)
    // ======================================================================
    // ChatController.java 내부
    @GetMapping("/chat/create")
    public String createRoom(
            @RequestParam(value = "seekerId", required = false) Long targetSeekerId,
            @RequestParam(value = "recruiterId", required = false) Long targetRecruiterId,
            @RequestParam("jobPostId") Long jobPostId,
            @RequestParam("jobSource") String jobSource, // ⬅️ 추가됨!
            @org.springframework.security.core.annotation.AuthenticationPrincipal net.kumo.kumo.security.AuthenticatedUser authUser) {

        UserEntity currentUser = userRepository.findByEmail(authUser.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("로그인 정보를 찾을 수 없습니다."));
        Long myId = currentUser.getUserId();

        Long finalSeekerId;
        Long finalRecruiterId;

        // 내가 구인자인지 구직자인지 판별
        if ("RECRUITER".equals(currentUser.getRole().name())) {
            finalRecruiterId = myId;
            finalSeekerId = targetSeekerId;
        } else {
            finalSeekerId = myId;
            finalRecruiterId = targetRecruiterId;
        }

        // Service에 Source도 같이 넘겨서 방 생성
        ChatRoomEntity room = chatService.createOrGetChatRoom(finalSeekerId, finalRecruiterId, jobPostId, jobSource);

        return "redirect:/chat/room/" + room.getId() + "?userId=" + myId;
    }

    /*
    @GetMapping("/chat/create")
    public String createRoom(
            @RequestParam("recruiterId") Long recruiterId,
            @RequestParam(value = "jobPostId", required = false) Long jobPostId,
            @RequestParam("userId") Long seekerId) { // 현재 로그인한 구직자 ID

        // 1. 서비스에서 방을 찾거나 생성
        ChatRoomEntity room = chatService.createOrGetChatRoom(seekerId, recruiterId, jobPostId);

        // 2. 생성된(혹은 찾은) 방으로 즉시 이동
        return "redirect:/chat/room/" + room.getId() + "?userId=" + seekerId;
    }*/

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
}