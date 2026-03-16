package net.kumo.kumo.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import net.kumo.kumo.domain.entity.UserEntity;
import net.kumo.kumo.repository.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 개발 환경에서 로그인 및 세션 구성을 강제로 에뮬레이션하기 위한 테스트용 Controller 클래스입니다.
 * 주의: 실제 운영 환경(Production)에서는 접근이 제한되거나 삭제되어야 합니다.
 */
@Controller
@RequiredArgsConstructor
public class TestController {

    private final UserRepository userRepository;

    /**
     * DB의 식별자(ID) 1번 유저를 조회하여 강제로 세션에 로그인 상태로 주입합니다.
     * 보안 모듈 적용 전 특정 기능을 테스트하기 위해 사용됩니다.
     *
     * @param session 현재 사용자의 HttpSession 객체
     * @return 강제 로그인 처리 결과 메시지
     * @throws RuntimeException 식별자가 1인 유저가 DB에 존재하지 않을 경우 발생
     */
    @GetMapping("/test/login")
    @ResponseBody
    public String forceLogin(HttpSession session) {
        UserEntity testUser = userRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("테스트용 유저(ID=1)가 DB에 존재하지 않습니다. 먼저 회원가입을 진행해주세요."));

        session.setAttribute("loginUser", testUser);

        return "개발 모드 강제 로그인 완료 (User ID: " + testUser.getUserId() + ") - 세션 주입이 완료되었습니다.";
    }

    /**
     * 현재 세션을 강제로 만료(무효화)시켜 로그아웃 상태를 에뮬레이션합니다.
     *
     * @param session 현재 사용자의 HttpSession 객체
     * @return 로그아웃 처리 결과 메시지
     */
    @GetMapping("/test/logout")
    @ResponseBody
    public String forceLogout(HttpSession session) {
        session.invalidate();
        return "개발 모드 강제 로그아웃 완료 - 세션이 무효화되었습니다.";
    }
}