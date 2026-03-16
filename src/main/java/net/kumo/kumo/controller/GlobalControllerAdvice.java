package net.kumo.kumo.controller;

import java.security.Principal;
import java.time.LocalDate;
import java.time.Period;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import net.kumo.kumo.domain.entity.UserEntity;
import net.kumo.kumo.service.RecruiterService;

/**
 * 모든 뷰(View) 템플릿에 공통으로 필요한 데이터(사용자 정보 등)를
 * 전역적으로 주입하기 위한 ControllerAdvice 클래스입니다.
 */
@ControllerAdvice
public class GlobalControllerAdvice {

    @Autowired
    private RecruiterService rs;

    /**
     * 현재 인증된 사용자의 기본 정보, 나이, 생년월일 분리 데이터,
     * 프로필 이미지 경로 등을 가공하여 Model에 공통 속성으로 주입합니다.
     *
     * @param model     전역 뷰에 전달할 Model 객체
     * @param principal 현재 인증된 사용자 정보
     */
    @ModelAttribute
    public void addAttributes(Model model, Principal principal) {
        if (principal == null) {
            return;
        }

        try {
            String email = principal.getName();
            UserEntity user = rs.getCurrentUser(email);

            if (user != null) {
                // 1. 유저 엔티티 객체 전달
                model.addAttribute("user", user);

                // 2. 전체 이름(FullName) 가공 및 전달
                String fullName = (user.getNameKanjiSei() != null ? user.getNameKanjiSei() : "")
                        + " "
                        + (user.getNameKanjiMei() != null ? user.getNameKanjiMei() : "");
                model.addAttribute("fullName", fullName.trim());

                // 3. 만 나이 계산 및 전달
                if (user.getBirthDate() != null) {
                    int age = Period.between(user.getBirthDate(), LocalDate.now()).getYears();
                    model.addAttribute("age", age);
                } else {
                    model.addAttribute("age", 0);
                }

                // 4. 연/월/일 분리 데이터 가공 및 전달
                if (user.getBirthDate() != null) {
                    String birthStr = user.getBirthDate().toString().replace("-", "");
                    if (birthStr.length() >= 8) {
                        model.addAttribute("birthYear", birthStr.substring(0, 4));
                        model.addAttribute("birthMonth", birthStr.substring(4, 6));
                        model.addAttribute("birthDay", birthStr.substring(6, 8));
                    }
                } else {
                    model.addAttribute("birthYear", "");
                    model.addAttribute("birthMonth", "");
                    model.addAttribute("birthDay", "");
                }

                // 5. 프로필 이미지 URL 주입
                if (user.getProfileImage() != null) {
                    model.addAttribute("profileImageUrl", user.getProfileImage().getFileUrl());
                } else {
                    model.addAttribute("profileImageUrl", null);
                }
            }
        } catch (Exception e) {
            // 전역 데이터 세팅 중 발생하는 예외는 서버 로직 흐름을 끊지 않도록 로깅만 수행
            System.out.println("[Global Data Binding Error] 공통 속성 주입 중 오류 발생: " + e.getMessage());
        }
    }
}