package net.kumo.kumo.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.kumo.kumo.domain.dto.FindPwDTO;
import net.kumo.kumo.service.EmailService;
import net.kumo.kumo.service.LoginService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * 비밀번호 찾기를 위한 이메일 인증 번호 발송 및 검증 요청을 처리하는 Controller 클래스입니다.
 */
@Slf4j
@RequiredArgsConstructor
@Controller
public class MailController {

    private final EmailService emailService;
    private final LoginService loginService;

    /**
     * 사용자 정보를 검증한 후, 등록된 이메일로 인증 번호를 발송합니다.
     * 생성된 인증 번호는 세션에 3분간 저장됩니다.
     *
     * @param findPwDTO 사용자 이름, 연락처, 이메일, 권한 정보가 담긴 DTO
     * @param session   인증 번호를 임시 저장할 HttpSession 객체
     * @return 이메일 발송 처리 결과 문자열을 포함한 ResponseEntity
     */
    @PostMapping("/api/mail/send")
    public ResponseEntity<String> sendCertificationMail(@RequestBody FindPwDTO findPwDTO, HttpSession session) {
        String name = findPwDTO.getName();
        String contact = findPwDTO.getContact();
        String email = findPwDTO.getEmail();
        String role = findPwDTO.getRole();

        log.info("비밀번호 찾기 메일 발송 요청 - Name: {}, Contact: {}, Email: {}, Role: {}", name, contact, email, role);

        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().body("EMPTY_EMAIL");
        }

        boolean isUserValid = loginService.emailVerify(name, contact, email, role);
        log.info("사용자 정보 유효성 검증 결과: {}", isUserValid);

        if (!isUserValid) {
            return ResponseEntity.badRequest().body("USER_NOT_FOUND");
        }

        try {
            String code = emailService.sendCertigicationMail(email);

            session.setAttribute("verifyCode", code);
            session.setMaxInactiveInterval(180); // 3분 만료

            return ResponseEntity.ok("SUCCESS");

        } catch (Exception e) {
            log.error("메일 서버 발송 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("MAIL_SEND_ERROR");
        }
    }

    /**
     * 사용자가 입력한 인증 번호와 세션에 저장된 인증 번호를 비교하여 검증합니다.
     * 검증에 성공하면 세션에서 인증 번호를 파기합니다.
     *
     * @param request 사용자가 입력한 인증 번호("code")가 포함된 Map
     * @param session 발송된 인증 번호가 저장된 HttpSession 객체
     * @return 인증 성공 여부(true/false)를 포함한 ResponseEntity
     */
    @PostMapping("/api/mail/check")
    public ResponseEntity<Boolean> checkCode(@RequestBody Map<String, String> request, HttpSession session) {
        String inputCode = request.get("code");
        String sessionCode = (String) session.getAttribute("verifyCode");

        if (sessionCode != null && sessionCode.equals(inputCode)) {
            session.removeAttribute("verifyCode");
            log.info("이메일 인증 번호 검증 성공 - SessionCode: {}, InputCode: {}", sessionCode, inputCode);
            return ResponseEntity.ok(true);
        }

        log.warn("이메일 인증 번호 검증 실패 또는 만료");
        return ResponseEntity.ok(false);
    }
}