package net.kumo.kumo.controller;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import net.kumo.kumo.domain.dto.ChatMessageDTO;
import net.kumo.kumo.domain.dto.ChatRoomListDTO;
import net.kumo.kumo.domain.entity.ChatRoomEntity;
import net.kumo.kumo.domain.entity.UserEntity;
import net.kumo.kumo.repository.ChatRoomRepository;
import net.kumo.kumo.repository.UserRepository;
import net.kumo.kumo.service.ChatService;
import net.kumo.kumo.service.MapService;

/**
 * 채팅방 관리 및 웹소켓 메시지 라우팅을 담당하는 컨트롤러입니다.
 * HTTP 요청(채팅방 입장, 파일 업로드 등)과 STOMP 기반의 웹소켓 메시지를 함께 처리합니다.
 */
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ChatRoomRepository chatRoomRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;
    private final MapService mapService;

    @Value("${file.upload.chat}")
    private String chatUploadDir;

    /**
     * 공고나 스카우트 제안을 통해 새로운 채팅방을 생성하거나 기존 방으로 리다이렉트합니다.
     * 언어(lang) 파라미터를 리다이렉트 URL에 포함하여 다국어 상태를 유지합니다.
     *
     * @param targetSeekerId 대상 구직자 ID (옵션)
     * @param targetRecruiterId 대상 구인자 ID (옵션)
     * @param jobPostId 대상 공고 ID
     * @param jobSource 공고 출처 (예: OSAKA, TOKYO)
     * @param lang 사용자 언어 설정 (기본값: "kr")
     * @param authUser 현재 로그인된 사용자 정보
     * @return 생성된 채팅방 입장 URL로의 리다이렉트 문자열
     */
    @GetMapping("/chat/create")
    public String createRoom(
            @RequestParam(value = "seekerId", required = false) Long targetSeekerId,
            @RequestParam(value = "recruiterId", required = false) Long targetRecruiterId,
            @RequestParam("jobPostId") Long jobPostId,
            @RequestParam("jobSource") String jobSource,
            @RequestParam(value = "lang", defaultValue = "kr") String lang,
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

        return "redirect:/chat/room/" + room.getId() + "?userId=" + myId + "&lang=" + lang;
    }

    /**
     * 특정 채팅방에 입장하여 과거 대화 기록 및 공고 정보를 모델에 담아 반환합니다.
     * 접속한 사용자의 언어(쿠키 기반)를 감지하여 급여(wage)와 날짜 포맷을 다국어로 분기합니다.
     *
     * @param roomId 입장할 채팅방 ID
     * @param userId 현재 접속하는 사용자의 ID
     * @param model 뷰에 전달할 데이터를 담는 모델 객체
     * @return 채팅방 HTML 뷰 이름
     */
    @GetMapping("/chat/room/{roomId}")
    public String enterRoom(@PathVariable Long roomId,
                            @RequestParam("userId") Long userId,
                            Model model) {

        ChatRoomEntity room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));

        String currentLang = org.springframework.context.i18n.LocaleContextHolder.getLocale().getLanguage();

        UserEntity opponent = room.getSeeker().getUserId().equals(userId) ? room.getRecruiter() : room.getSeeker();
        model.addAttribute("roomName", opponent.getNickname());

        String oppImgUrl = "/images/common/default_profile.png";
        if (opponent.getProfileImage() != null && opponent.getProfileImage().getFileUrl() != null) {
            oppImgUrl = opponent.getProfileImage().getFileUrl();
        }
        model.addAttribute("opponentProfileImg", oppImgUrl);

        List<net.kumo.kumo.domain.dto.ChatMessageDTO> history = chatService.getMessageHistory(roomId, userId, currentLang);
        model.addAttribute("chatHistory", history);

        model.addAttribute("roomId", roomId);
        model.addAttribute("userId", userId);
        model.addAttribute("lang", currentLang);

        return "chat/chat_room";
    }

    /**
     * 로그인된 사용자의 소속된 모든 채팅방 목록 화면을 반환합니다.
     *
     * @param userId 사용자 ID (명시되지 않을 경우 Security Context에서 추출)
     * @param authUser 현재 로그인된 사용자 정보
     * @param model 뷰에 전달할 모델 객체
     * @return 채팅 목록 HTML 뷰 이름
     */
    @GetMapping("/chat/list")
    public String chatList(
            @RequestParam(value = "userId", required = false) Long userId,
            @org.springframework.security.core.annotation.AuthenticationPrincipal net.kumo.kumo.security.AuthenticatedUser authUser,
            Model model) {

        if (userId == null && authUser != null) {
            UserEntity currentUser = userRepository.findByEmail(authUser.getEmail())
                    .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));
            userId = currentUser.getUserId();
        }

        if (userId == null) {
            return "redirect:/login";
        }

        List<ChatRoomListDTO> chatRooms = chatService.getChatRoomsForUser(userId);

        model.addAttribute("chatRooms", chatRooms);
        model.addAttribute("userId", userId);

        return "chat/chat_list";
    }

    /**
     * 채팅창 내에서 전송된 이미지 및 문서 파일을 서버 디렉토리에 저장합니다.
     *
     * @param file 업로드된 MultipartFile 객체
     * @return 성공 시 저장된 파일의 URL, 실패 시 에러 메시지를 포함한 ResponseEntity
     */
    @PostMapping("/chat/upload")
    @ResponseBody
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty())
                return ResponseEntity.badRequest().body("파일이 없습니다.");

            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null)
                return ResponseEntity.badRequest().body("파일명 오류");

            String ext = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
            List<String> allowedExts = Arrays.asList("jpg", "jpeg", "png", "gif", "webp", "avif", "pdf", "docx", "doc",
                    "xlsx", "xls", "txt");

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
            if (!folder.exists())
                folder.mkdirs();

            File dest = new File(fullPath + savedFilename);
            file.transferTo(dest);

            return ResponseEntity.ok("/chat_images/" + savedFilename);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("업로드 실패");
        }
    }

    /**
     * 웹소켓을 통해 클라이언트로부터 수신된 채팅 메시지를 저장하고,
     * 해당 방에 참여 중인 클라이언트 및 로비(목록 갱신용) 구독자들에게 브로드캐스팅합니다.
     *
     * @param messageDTO 클라이언트가 전송한 메시지 데이터 객체
     */
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

    /**
     * 상대방이 채팅방에 들어왔을 때 전송되는 '읽음' 신호를 처리하고
     * 같은 방의 클라이언트들에게 브로드캐스팅하여 UI(안 읽음 뱃지 등)를 갱신합니다.
     *
     * @param readSignal 클라이언트가 전송한 읽음 신호 DTO
     */
    @MessageMapping("/chat/read")
    public void processRead(ChatMessageDTO readSignal) {
        chatService.processLiveReadSignal(readSignal.getRoomId(), readSignal.getSenderId());
        messagingTemplate.convertAndSend("/sub/chat/room/" + readSignal.getRoomId(), readSignal);
    }

    /**
     * 현재 로그인된 사용자의 읽지 않은 총 메시지 개수를 반환하는 API입니다.
     * 메인 페이지나 네비게이션 바 등의 전역 알림 표시에 사용됩니다.
     *
     * @param authUser 현재 로그인된 사용자 정보
     * @return 읽지 않은 메시지 개수 정수값
     */
    @GetMapping("/api/chat/unread-count")
    @ResponseBody
    public ResponseEntity<Integer> getUnreadCount(
            @org.springframework.security.core.annotation.AuthenticationPrincipal net.kumo.kumo.security.AuthenticatedUser authUser) {

        if (authUser == null) {
            return ResponseEntity.status(401).build();
        }

        UserEntity currentUser = userRepository.findByEmail(authUser.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

        int unreadCount = chatService.getUnreadMessageCount(currentUser.getUserId());

        return ResponseEntity.ok(unreadCount);
    }
}