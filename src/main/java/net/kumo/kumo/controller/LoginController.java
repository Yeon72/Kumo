package net.kumo.kumo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.kumo.kumo.domain.dto.ChangeNewPWDTO;
import net.kumo.kumo.domain.dto.JoinRecruiterDTO;
import net.kumo.kumo.domain.dto.JoinSeekerDTO;
import net.kumo.kumo.service.LoginService;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 로그인, 회원가입(구인자/구직자), 아이디/비밀번호 찾기 등
 * 인증 관련 뷰 라우팅 및 폼 제출 처리를 담당하는 Controller 클래스입니다.
 */
@Slf4j
@RequiredArgsConstructor
@Controller
public class LoginController {

    private final LoginService loginService;

    @Value("${file.upload.dir}")
    private String uploadDir;

    @Value("${kumo.google.maps.keys}")
    private String googleMapsKey;

    @GetMapping("login")
    public String login() {
        return "NonLoginView/login";
    }

    @GetMapping("join")
    public String join() {
        return "NonLoginView/join";
    }

    @GetMapping("FindId")
    public String findId() {
        return "NonLoginView/FindId";
    }

    @GetMapping("FindPw")
    public String findPw() {
        return "NonLoginView/FindPw";
    }

    /**
     * 구직자 회원가입 폼 페이지를 렌더링합니다.
     *
     * @param model 뷰에 전달할 Model 객체 (Google Maps Key 포함)
     * @return 구직자 회원가입 뷰 파일명
     */
    @GetMapping("/join/seeker")
    public String joinSeekerForm(Model model) {
        model.addAttribute("googleKey", googleMapsKey);
        return "NonLoginView/joinSeeker";
    }

    /**
     * 구직자 회원가입 정보를 처리하고 저장합니다.
     *
     * @param dto 회원가입 폼 데이터가 바인딩된 DTO
     * @return 처리 완료 후 로그인 페이지 리다이렉트
     */
    @PostMapping("/join/seeker")
    public String joinSeeker(@ModelAttribute JoinSeekerDTO dto) {
        log.info("구직자 회원가입 요청 데이터: {}", dto);
        loginService.insertSeeker(dto);
        return "redirect:/login";
    }

    /**
     * 구인자 회원가입 폼 페이지를 렌더링합니다.
     *
     * @param model 뷰에 전달할 Model 객체 (Google Maps Key 포함)
     * @return 구인자 회원가입 뷰 파일명
     */
    @GetMapping("/join/recruiter")
    public String joinRecruiterForm(Model model) {
        model.addAttribute("googleKey", googleMapsKey);
        return "NonLoginView/joinRecruiter";
    }

    /**
     * 구인자 회원가입 정보 및 사업자 증빙 서류 업로드를 처리합니다.
     *
     * @param dto 증빙 서류 파일(MultipartFile)을 포함한 회원가입 폼 데이터 DTO
     * @return 성공 시 승인 대기 페이지 리다이렉트, 실패 시 에러 파라미터와 함께 가입 폼 리다이렉트
     */
    @PostMapping("/join/recruiter")
    public String joinRecruiterProcess(@ModelAttribute JoinRecruiterDTO dto) {
        log.info("구인자 가입 요청 수신 - 계정: {}", dto.getEmail());
        List<String> savedFileNames = new ArrayList<>();

        try {
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                if (created) {
                    log.info("증빙 서류 업로드 폴더 생성 완료: {}", uploadDir);
                }
            }

            List<MultipartFile> files = dto.getEvidenceFiles();
            if (files != null) {
                for (MultipartFile file : files) {
                    if (file.isEmpty()) continue;

                    String originalFilename = file.getOriginalFilename();
                    String savedFilename = UUID.randomUUID() + "_" + originalFilename;

                    String fullPath = uploadDir.endsWith("/")
                            ? uploadDir + savedFilename
                            : uploadDir + "/" + savedFilename;

                    file.transferTo(new File(fullPath));
                    savedFileNames.add(savedFilename);
                    log.info("증빙 서류 파일 저장 완료: {}", savedFilename);
                }
            }

            loginService.joinRecruiter(dto, savedFileNames);
            return "redirect:/join/wait";

        } catch (IOException e) {
            log.error("증빙 서류 파일 업로드 중 I/O 에러 발생", e);
            return "redirect:/join/recruiter?error=upload";
        } catch (Exception e) {
            log.error("구인자 회원가입 비즈니스 로직 처리 중 에러 발생", e);
            return "redirect:/join/recruiter?error=fail";
        }
    }

    /**
     * 구인자 회원가입 완료 후 관리자 승인 대기 안내 페이지를 렌더링합니다.
     *
     * @return 대기 안내 뷰 파일명
     */
    @GetMapping("/join/wait")
    public String joinWait() {
        return "NonLoginView/joinWait";
    }

    /**
     * 비밀번호 재설정을 위한 이메일 인증 통과 후, 새 비밀번호 입력 페이지를 렌더링합니다.
     *
     * @param email 인증된 사용자의 이메일
     * @param model 뷰에 이메일 정보를 전달하기 위한 Model 객체
     * @return 비밀번호 변경 폼 뷰 파일명
     */
    @PostMapping("/changePw")
    public String changePwForm(@RequestParam("email") String email, Model model) {
        model.addAttribute("email", email);
        return "NonLoginView/changePw";
    }

    /**
     * 새로운 비밀번호로 계정 정보를 갱신합니다.
     *
     * @param changeNewPWDTO 이메일 및 새 비밀번호가 포함된 DTO
     * @return 처리 완료 후 로그인 페이지 리다이렉트
     */
    @PostMapping("ChangeNewPW")
    public String updateNewPw(@ModelAttribute ChangeNewPWDTO changeNewPWDTO) {
        log.info("비밀번호 변경 요청 수신 - 계정: {}", changeNewPWDTO.getEmail());
        loginService.ChangeNewPW(changeNewPWDTO);
        return "redirect:/login";
    }
}