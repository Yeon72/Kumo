package net.kumo.kumo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 서비스의 메인 진입점(Home) 및 기본 안내 페이지 라우팅을 담당하는 Controller 클래스입니다.
 */
@Slf4j
@RequiredArgsConstructor
@Controller
public class HomeController {

    /**
     * 메인 홈 화면을 렌더링합니다.
     *
     * @return 홈 뷰 파일명
     */
    @GetMapping({ "", "/" })
    public String home() {
        return "home";
    }

    /**
     * 서비스 소개 및 안내 페이지를 렌더링합니다.
     *
     * @return 안내 페이지 뷰 파일명
     */
    @GetMapping("/info")
    public String info() {
        return "NonLoginView/info";
    }
}