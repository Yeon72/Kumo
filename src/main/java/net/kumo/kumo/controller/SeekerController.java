package net.kumo.kumo.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.kumo.kumo.domain.dto.JoinSeekerDTO;
import net.kumo.kumo.domain.dto.ResumeDto;
import net.kumo.kumo.domain.dto.SeekerMyPageDTO;
import net.kumo.kumo.domain.entity.UserEntity;
import net.kumo.kumo.service.SeekerService;
import org.springframework.context.MessageSource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Locale;

@RequestMapping("/Seeker")
@Slf4j
@RequiredArgsConstructor
@Controller
public class SeekerController {
    private final SeekerService seekerService;
	private final MessageSource messageSource; // 🌟 다국어 메시지 처리를 위해 추가

    @GetMapping("/MyPage")
    public String SeekerMyPage(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        SeekerMyPageDTO dto = seekerService.getDTO(userDetails.getUsername());
        model.addAttribute("user", dto);

        return "SeekerView/MyPage";
    }
	
	@GetMapping("/ProfileEdit")
	public String SeekerProfileEdit(Model model){
		return "SeekerView/SeekerProfileEdit";
	}
	
	@PostMapping("/ProfileEdit")
	public String SeekerPrfileEdit(@ModelAttribute JoinSeekerDTO dto){
		
		log.info("dto, 잘들어옴? : {}",dto);
		
		seekerService.updateProfile(dto);
		return "redirect:/Seeker/MyPage";
	}
	
	@GetMapping("/resume")
	public String SeekerResume(Model model, @AuthenticationPrincipal UserDetails userDetails){
		
		SeekerMyPageDTO dto = seekerService.getDTO(userDetails.getUsername());
		model.addAttribute("user", dto);
		
		return "SeekerView/SeekerResume";
	}
	
	@PostMapping("/resume")
	public String submitResume(@ModelAttribute ResumeDto dto, BindingResult bindingResult,
							   @AuthenticationPrincipal UserDetails user, RedirectAttributes rttr, Locale locale){
		log.info("==== 이력서 제출 시도 ====");
		
		if (bindingResult.hasErrors()) {
			log.error("==== 데이터 바인딩 에러 발생! ====");
			bindingResult.getAllErrors().forEach(error -> {
				log.error("에러 필드/메시지: {}", error.toString());
			});
			return "SeekerView/SeekerResume";
		}
		
		try {
			seekerService.saveResume(dto, user.getUsername());
			// 🌟 메시지를 여기서 직접 번역해서 보냄
			String msg = messageSource.getMessage("resume.msg.saveSuccess", null, locale);
			rttr.addFlashAttribute("successMessage", msg);
		}catch (Exception e){
			log.error("이력서 저장 중 에러 발생: {}", e.getMessage());
			String msg = messageSource.getMessage("resume.msg.saveFail", null, locale);
			rttr.addFlashAttribute("errorMessage", msg);
			return "redirect:/Seeker/resume";
		}
		
		return "redirect:/Seeker/MyPage";
	
	}
	

}
