package net.kumo.kumo.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.kumo.kumo.domain.dto.FindIdDTO;
import net.kumo.kumo.domain.dto.ProfileImageUploadDTO;
import net.kumo.kumo.service.LoginService;
import net.kumo.kumo.service.SeekerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 프론트엔드와 비동기 통신(AJAX/Fetch)을 수행하는 공통 API Controller 클래스입니다.
 * 회원가입 유효성 검사, 아이디 찾기, 파일 업로드 등의 로직을 처리합니다.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class ApiController {

    private final LoginService loginService;
    private final SeekerService seekerService;

    /**
     * 회원가입 시 닉네임 중복 여부를 검사합니다.
     *
     * @param request 검사할 닉네임 문자열 데이터
     * @return 중복일 경우 true, 사용 가능할 경우 false
     */
    @PostMapping("/api/check/nickname")
    public ResponseEntity<Boolean> checkNickname(@RequestBody Map<String, String> request) {
        String nickname = request.get("nickname");
        boolean exists = loginService.existsByNickname(nickname);
        return ResponseEntity.ok(exists);
    }

    /**
     * 회원가입 시 이메일 중복 여부를 검사합니다.
     *
     * @param request 검사할 이메일 문자열 데이터
     * @return 중복일 경우 true, 사용 가능할 경우 false
     */
    @PostMapping("/api/check/email")
    public ResponseEntity<Boolean> checkEmail(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        boolean exists = loginService.existsByEmail(email);
        return ResponseEntity.ok(exists);
    }

    /**
     * 이름과 전화번호를 기반으로 사용자 이메일(아이디)을 조회합니다.
     * 보안을 위해 결과 이메일은 부분적으로 마스킹 처리되어 반환됩니다.
     *
     * @param findIdDTO 조회에 필요한 회원 정보 DTO
     * @return 마스킹된 이메일 정보와 결과 메시지를 포함한 응답 객체
     */
    @PostMapping("/api/findId")
    public ResponseEntity<Map<String, Object>> findIdProc(@RequestBody FindIdDTO findIdDTO) {
        log.info("아이디 찾기 요청 수신 - 데이터: {}", findIdDTO);
        Map<String, Object> response = new HashMap<>();

        String foundEmail = loginService.findId(findIdDTO);

        if (foundEmail != null) {
            String maskedEmail = maskEmail(foundEmail);

            response.put("status", "success");
            response.put("email", maskedEmail);
            response.put("message", "일치하는 정보를 찾았습니다.");
        } else {
            response.put("status", "fail");
            response.put("message", "일치하는 회원 정보가 없습니다.");
        }

        return ResponseEntity.ok(response);
    }

    /**
     * 문자열 이메일 주소의 로컬 파트(아이디 영역)를 보안 정책에 따라 마스킹 처리합니다.
     *
     * @param email 원본 이메일 주소
     * @return 마스킹된 이메일 주소
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }

        String[] parts = email.split("@");
        String id = parts[0];
        String domain = parts[1];
        int len = id.length();

        String maskedId;

        if (len <= 2) {
            maskedId = id.charAt(0) + "*".repeat(len - 1);
        } else if (len == 3) {
            maskedId = id.charAt(0) + "*" + id.charAt(2);
        } else {
            String head = id.substring(0, 3);
            String tail = id.substring(len - 2);
            maskedId = head + "****" + tail;
        }

        return maskedId + "@" + domain;
    }

    /**
     * 사용자의 프로필 이미지를 업로드하고 경로를 갱신합니다.
     *
     * @param dto     업로드할 이미지 파일 데이터
     * @param details 현재 인증된 사용자 정보
     * @return 업로드된 파일의 서버 접근 경로
     */
    @PostMapping("/api/profileImage")
    public ResponseEntity<String> uploadProfileImage(
            @ModelAttribute ProfileImageUploadDTO dto,
            @AuthenticationPrincipal UserDetails details){
        try{
            log.info("프로필 이미지 업로드 요청 수신 - 파일 정보: {}", dto);
            String newImagePath = seekerService.updateProfileImage(details.getUsername(), dto.getProfileImage());

            return ResponseEntity.ok(newImagePath);
        }catch (Exception e){
            log.error("프로필 이미지 업로드 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("파일 업로드 실패");
        }
    }

    /**
     * 사용자의 회원 탈퇴 처리를 수행합니다.
     *
     * @param request     비밀번호 검증을 위한 요청 데이터
     * @param userDetails 현재 인증된 사용자 정보
     * @return 탈퇴 성공 여부를 나타내는 응답 객체
     */
    @PostMapping("/api/user/delete")
    public ResponseEntity<?> deleteAccount(
            @RequestBody Map<String,String> request,
            @AuthenticationPrincipal UserDetails userDetails){
        String email = userDetails.getUsername();
        String rawPassword = request.get("password");

        log.info("회원 탈퇴 요청 수신 - 계정: {}", email);

        try {
            boolean isDeleted = loginService.deleteAccount(email, rawPassword);

            if(!isDeleted){
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("비밀번호가 일치하지 않습니다.");
            }

            return ResponseEntity.ok("탈퇴 처리가 완료되었습니다.");
        }catch (Exception e){
            log.error("회원 탈퇴 처리 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 내부 오류가 발생했습니다.");
        }
    }
}