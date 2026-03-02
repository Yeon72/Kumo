package net.kumo.kumo.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.kumo.kumo.domain.dto.JoinSeekerDTO;
import net.kumo.kumo.domain.dto.ResumeDto;
import net.kumo.kumo.domain.dto.SeekerApplicationHistoryDTO;
import net.kumo.kumo.domain.dto.SeekerMyPageDTO;
import net.kumo.kumo.domain.entity.ScoutOfferEntity;
import net.kumo.kumo.domain.entity.UserEntity;
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

@RequestMapping("/Seeker")
@Slf4j
@RequiredArgsConstructor
@Controller
public class SeekerController {
    private final SeekerService seekerService;
	private final MessageSource messageSource;
    private final ScoutOfferRepository scoutOfferRepo;

    @GetMapping("/MyPage")
    public String SeekerMyPage(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        SeekerMyPageDTO dto = seekerService.getDTO(userDetails.getUsername());
        model.addAttribute("user", dto);
        return "SeekerView/MyPage";
    }

    /**
     * 스카우트 제의 목록 조회
     */
    @GetMapping("/scout")
    public String SeekerScout(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        List<ScoutOfferEntity> offers = seekerService.getScoutOffers(userDetails.getUsername());
        
        model.addAttribute("offers", offers);
        model.addAttribute("currentMenu", "scout"); // 사이드바 활성화용
        return "SeekerView/SeekerScout";
    }
	
    /**
     * 스카우트 제의 삭제 (거절/숨김)
     */
    @PostMapping("/scout/delete")
    @ResponseBody
    public String deleteScoutOffer(@RequestParam("scoutId") Long scoutId) {
        try {
            scoutOfferRepo.deleteById(scoutId);
            return "success";
        } catch (Exception e) {
            return "fail";
        }
    }

    @GetMapping("/ProfileEdit")
	public String SeekerProfileEdit(Model model){
		return "SeekerView/SeekerProfileEdit";
	}
	
	@PostMapping("/ProfileEdit")
	public String SeekerPrfileEdit(@ModelAttribute JoinSeekerDTO dto){
		seekerService.updateProfile(dto);
		return "redirect:/Seeker/MyPage";
	}
	
	@GetMapping("/resume")
	public String SeekerResume(Model model, @AuthenticationPrincipal UserDetails userDetails){
		SeekerMyPageDTO userDto = seekerService.getDTO(userDetails.getUsername());
		ResumeDto resumeDto = seekerService.getResume(userDetails.getUsername());
		model.addAttribute("user", userDto);
		model.addAttribute("resume", resumeDto);
		return "SeekerView/SeekerResume";
	}

	@PostMapping("/resume")
	public String submitResume(@ModelAttribute ResumeDto dto, BindingResult bindingResult,
							   @AuthenticationPrincipal UserDetails user, RedirectAttributes rttr, Locale locale){
		if (bindingResult.hasErrors()) {
			return "SeekerView/SeekerResume";
		}
		try {
			seekerService.saveResume(dto, user.getUsername());
			String msg = messageSource.getMessage("resume.msg.saveSuccess", null, locale);
			rttr.addFlashAttribute("successMessage", msg);
		}catch (Exception e){
			String msg = messageSource.getMessage("resume.msg.saveFail", null, locale);
			rttr.addFlashAttribute("errorMessage", msg);
			return "redirect:/Seeker/resume";
		}
		return "redirect:/Seeker/MyPage";
	}
	
	@GetMapping("/history")
	public String SeekerHistory(Model model, @AuthenticationPrincipal UserDetails userDetails){
        List<SeekerApplicationHistoryDTO> history = seekerService.getApplicationHistory(userDetails.getUsername());
        model.addAttribute("history", history);
        model.addAttribute("currentMenu", "history");
	    return "SeekerView/SeekerHistory";
	}

    /**
     * 지원 취소 API
     */
    @PostMapping("/history/cancel")
    @ResponseBody
    public String cancelApplication(@RequestParam("appId") Long appId, @AuthenticationPrincipal UserDetails userDetails) {
        try {
            seekerService.cancelApplication(appId, userDetails.getUsername());
            return "success";
        } catch (Exception e) {
            return "fail";
        }
    }
}
