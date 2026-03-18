package net.kumo.kumo.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.kumo.kumo.domain.dto.JoinSeekerDTO;
import net.kumo.kumo.domain.dto.ResumeDto;
import net.kumo.kumo.domain.dto.SeekerApplicationHistoryDTO;
import net.kumo.kumo.domain.dto.SeekerMyPageDTO;
import net.kumo.kumo.domain.entity.ScoutOfferEntity;
import net.kumo.kumo.repository.ScoutOfferRepository;
import net.kumo.kumo.service.SeekerService;
import org.springframework.context.MessageSource;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Locale;

/**
 * 구직자(Seeker) 전용 뷰 라우팅 및 비즈니스 로직(마이페이지, 이력서, 지원 내역 등)을 처리하는 Controller 클래스입니다.
 */
@RequestMapping("/Seeker")
@Slf4j
@RequiredArgsConstructor
@Controller
public class SeekerController {

    private final SeekerService seekerService;
    private final MessageSource messageSource;
    private final ScoutOfferRepository scoutOfferRepo;

    /**
     * 구직자 마이페이지 메인 화면을 렌더링합니다.
     *
     * @param model       뷰에 전달할 Model 객체
     * @param userDetails 현재 인증된 구직자 정보
     * @return 구직자 마이페이지 뷰 파일명
     */
    @GetMapping("/MyPage")
    public String seekerMyPage(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        SeekerMyPageDTO dto = seekerService.getDTO(userDetails.getUsername());
        model.addAttribute("user", dto);
        return "SeekerView/MyPage";
    }

    /**
     * 구인자로부터 받은 스카우트 제의 목록 화면을 렌더링합니다.
     *
     * @param model       뷰에 전달할 Model 객체
     * @param userDetails 현재 인증된 구직자 정보
     * @return 구직자 스카우트 목록 뷰 파일명
     */
    @GetMapping("/scout")
    public String seekerScout(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        List<ScoutOfferEntity> offers = seekerService.getScoutOffers(userDetails.getUsername());

        model.addAttribute("offers", offers);
        model.addAttribute("currentMenu", "scout");
        return "SeekerView/SeekerScout";
    }

    /**
     * 특정 스카우트 제의 내역을 삭제(거절/숨김 처리)합니다.
     *
     * @param scoutId 삭제할 스카우트 제의 식별자
     * @return 처리 성공 여부 문자열 (success / fail)
     */
    @PostMapping("/scout/delete")
    @ResponseBody
    public String deleteScoutOffer(@RequestParam("scoutId") Long scoutId) {
        try {
            scoutOfferRepo.deleteById(scoutId);
            return "success";
        } catch (Exception e) {
            log.error("스카우트 제의 삭제 실패 - ID: {}", scoutId, e);
            return "fail";
        }
    }

    /**
     * 구직자 프로필 정보 수정 화면을 렌더링합니다.
     *
     * @param model 뷰에 전달할 Model 객체
     * @return 구직자 프로필 수정 뷰 파일명
     */
    @GetMapping("/ProfileEdit")
    public String seekerProfileEditForm(Model model){
        return "SeekerView/SeekerProfileEdit";
    }

    /**
     * 구직자 프로필 수정 정보를 저장하고 마이페이지로 리다이렉트합니다.
     *
     * @param dto 수정할 프로필 정보가 담긴 DTO
     * @return 마이페이지 리다이렉트 URL
     */
    @PostMapping("/ProfileEdit")
    public String seekerProfileEditProcess(@ModelAttribute JoinSeekerDTO dto){
        seekerService.updateProfile(dto);
        return "redirect:/Seeker/MyPage";
    }

    /**
     * 구직자 이력서 작성 및 수정 폼 화면을 렌더링합니다.
     * 기존 이력서 데이터가 있을 경우 함께 전달합니다.
     *
     * @param model       뷰에 전달할 Model 객체
     * @param userDetails 현재 인증된 구직자 정보
     * @return 구직자 이력서 관리 뷰 파일명
     */
    @GetMapping("/resume")
    public String seekerResumeForm(Model model, @AuthenticationPrincipal UserDetails userDetails){
        SeekerMyPageDTO userDto = seekerService.getDTO(userDetails.getUsername());
        ResumeDto resumeDto = seekerService.getResume(userDetails.getUsername());

        model.addAttribute("user", userDto);
        model.addAttribute("resume", resumeDto);
        return "SeekerView/SeekerResume";
    }

    /**
     * 작성 또는 수정된 이력서 데이터를 저장합니다.
     *
     * @param dto           이력서 데이터가 담긴 DTO
     * @param bindingResult 데이터 바인딩 오류 확인 객체
     * @param user          현재 인증된 구직자 정보
     * @param rttr          리다이렉트 시 전달할 플래시 속성 객체
     * @param locale        클라이언트 로케일 정보 (다국어 메시지용)
     * @return 처리 성공 시 마이페이지 리다이렉트, 실패 시 이력서 폼 리다이렉트
     */
    @PostMapping("/resume")
    public String submitResume(@ModelAttribute ResumeDto dto, BindingResult bindingResult,
                               @AuthenticationPrincipal UserDetails user, RedirectAttributes rttr, Locale locale){
        if (bindingResult.hasErrors()) {
            log.warn("이력서 저장 폼 데이터 바인딩 오류 발생");
            return "SeekerView/SeekerResume";
        }

        try {
            seekerService.saveResume(dto, user.getUsername());
            String msg = messageSource.getMessage("resume.msg.saveSuccess", null, locale);
            rttr.addFlashAttribute("successMessage", msg);
        } catch (Exception e){
            log.error("이력서 저장 실패 - 계정: {}", user.getUsername(), e);
            String msg = messageSource.getMessage("resume.msg.saveFail", null, locale);
            rttr.addFlashAttribute("errorMessage", msg);
            return "redirect:/Seeker/resume";
        }

        return "redirect:/Seeker/MyPage";
    }

    /**
     * 구직자의 역대 구인 공고 지원 내역 목록 화면을 렌더링합니다.
     *
     * @param model       뷰에 전달할 Model 객체
     * @param userDetails 현재 인증된 구직자 정보
     * @return 지원 내역 뷰 파일명
     */
    @GetMapping("/history")
    public String seekerHistory(Model model, @AuthenticationPrincipal UserDetails userDetails){
        List<SeekerApplicationHistoryDTO> history = seekerService.getApplicationHistory(userDetails.getUsername());
        model.addAttribute("history", history);
        model.addAttribute("currentMenu", "history");
        return "SeekerView/SeekerHistory";
    }

    /**
     * 특정 구인 공고 지원을 취소 처리합니다.
     *
     * @param appId       취소할 지원 내역(Application) 식별자
     * @param userDetails 현재 인증된 구직자 정보
     * @return 처리 성공 여부 문자열 (success / fail)
     */
    @PostMapping("/history/cancel")
    @ResponseBody
    public String cancelApplication(@RequestParam("appId") Long appId, @AuthenticationPrincipal UserDetails userDetails) {
        try {
            seekerService.cancelApplication(appId, userDetails.getUsername());
            return "success";
        } catch (Exception e) {
            log.error("지원 취소 처리 실패 - 지원내역 ID: {}", appId, e);
            return "fail";
        }
    }
}