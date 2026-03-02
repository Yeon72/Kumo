package net.kumo.kumo.controller;

import java.io.File;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.kumo.kumo.domain.dto.JobManageListDTO;
import net.kumo.kumo.domain.dto.JobPostingRequestDTO;
import net.kumo.kumo.domain.dto.JoinRecruiterDTO;
import net.kumo.kumo.domain.dto.ResumeDto;
import net.kumo.kumo.domain.entity.CompanyEntity;
import net.kumo.kumo.domain.entity.SeekerProfileEntity;
import net.kumo.kumo.domain.entity.UserEntity;
import net.kumo.kumo.repository.SeekerProfileRepository;
import net.kumo.kumo.repository.UserRepository;
import net.kumo.kumo.service.CompanyService;
import net.kumo.kumo.service.JobPostingService;
import net.kumo.kumo.service.RecruiterService;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("Recruiter")
@Controller
public class RecruiterController {

    private final UserRepository ur;
    private final RecruiterService rs;
    private final SeekerProfileRepository seekerProfileRepo;
    private final CompanyService cs;
    private final JobPostingService js;

    @GetMapping("Main")
    public String Main(Model model, Principal principal) {
        String userEmail = principal.getName();
        model.addAttribute("jobList", js.getMyJobPostings(userEmail));
        model.addAttribute("talents", rs.getScoutedProfiles());
        model.addAttribute("currentMenu", "home");
        return "recruiterView/main";
    }

    @GetMapping("TalentList")
    public String TalentList(Model model, Principal principal) {
        UserEntity currentUser = ur.findByEmail(principal.getName()).get();
        model.addAttribute("user", currentUser);
        model.addAttribute("talents", rs.getScoutedProfiles());
        model.addAttribute("currentMenu", "none");
        return "recruiterView/talentList";
    }

    @GetMapping("TalentDetail")
    public String TalentDetail(@RequestParam("userId") Long userId, Model model, Principal principal) {
        UserEntity currentUser = ur.findByEmail(principal.getName()).get();
        model.addAttribute("user", currentUser);
        model.addAttribute("talent", ur.findById(userId).get());
        model.addAttribute("resume", rs.getTalentResume(userId));
        model.addAttribute("profile", seekerProfileRepo.findByUser_UserId(userId).orElse(null));
        model.addAttribute("currentMenu", "none");
        return "recruiterView/talentDetail";
    }

    /**
     * 🌟 초간단 스카우트 제의 발송 (GET 방식)
     */
    @GetMapping("/sendOffer")
    public String sendOffer(@RequestParam("userId") Long seekerId, @AuthenticationPrincipal UserDetails userDetails, RedirectAttributes rttr) {
        try {
            rs.sendScoutOffer(userDetails.getUsername(), seekerId);
            rttr.addFlashAttribute("successMsg", "제의를 성공적으로 보냈습니다.");
        } catch (Exception e) {
            rttr.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/Recruiter/TalentDetail?userId=" + seekerId;
    }

    @GetMapping("ApplicantInfo")
    public String ApplicantInfo(Model model) {
        model.addAttribute("currentMenu", "applicants");
        return "recruiterView/applicantInfo";
    }

    @GetMapping("JobManage")
    public String JobManage(Model model, Principal principal) {
        model.addAttribute("currentMenu", "jobManage");
        model.addAttribute("jobList", js.getMyJobPostings(principal.getName()));
        return "recruiterView/jobManage";
    }

    @GetMapping("Calendar")
    public String Calender(Model model) {
        model.addAttribute("currentMenu", "calendar");
        return "recruiterView/calendar";
    }

    @GetMapping("/Settings")
    public String Settings(Model model) {
        model.addAttribute("currentMenu", "settings");
        return "recruiterView/settings";
    }

    @PostMapping("/UploadProfile")
    @ResponseBody
    public ResponseEntity<?> uploadProfile(@RequestParam("profileImage") MultipartFile file, Principal principal) {
        try {
            String uploadDir = System.getProperty("user.home") + "/kumo_uploads/profiles/";
            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs();
            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            file.transferTo(new File(uploadDir + fileName));
            rs.updateProfileImage(principal.getName(), "/upload/profiles/" + fileName, file.getOriginalFilename(), fileName, file.getSize());
            return ResponseEntity.ok().body(Map.of("success", true, "imageUrl", "/upload/profiles/" + fileName));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/ProfileEdit")
    public String ProfileEdit() { return "recruiterView/profileEdit"; }

    @PostMapping("/ProfileEdit")
    public String ProfileEdit(@ModelAttribute JoinRecruiterDTO dto) {
        rs.updateProfile(dto);
        return "redirect:/Recruiter/Settings";
    }

    @GetMapping("/JobPosting")
    public String jobPostingPage(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        model.addAttribute("companies", cs.getCompanyList(ur.findByEmail(userDetails.getUsername()).get()));
        return "recruiterView/jobPosting";
    }

    @PostMapping("/JobPosting")
    public String submitJobPosting(@ModelAttribute JobPostingRequestDTO dto, @RequestParam(value = "images", required = false) List<MultipartFile> images, @AuthenticationPrincipal UserDetails userDetails) {
        js.saveJobPosting(dto, images, ur.findByEmail(userDetails.getUsername()).get());
        return "redirect:/Recruiter/JobManage";
    }

    @DeleteMapping("/api/recruiter/postings")
    public ResponseEntity<?> deletePosting(@RequestParam("datanum") Long datanum, @RequestParam("region") String region, Principal principal) {
        try {
            js.deleteMyJobPosting(datanum, region, principal.getName());
            return ResponseEntity.ok().body("삭제 완료");
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @GetMapping("/editJobPosting")
    public String editJobPostingPage(@RequestParam("id") Long id, @RequestParam("region") String region, Model model, @AuthenticationPrincipal UserDetails userDetails) {
        model.addAttribute("job", js.getJobPostingForEdit(id, region));
        model.addAttribute("companies", cs.getCompanyList(ur.findByEmail(userDetails.getUsername()).get()));
        model.addAttribute("region", region);
        model.addAttribute("jobId", id);
        return "recruiterView/editJobPosting";
    }

    @PostMapping("/editJobPosting")
    public String updateJobPosting(@RequestParam("id") Long id, @RequestParam("region") String region, @ModelAttribute JobPostingRequestDTO dto, @RequestParam(value = "images", required = false) List<MultipartFile> images) {
        js.updateJobPosting(id, region, dto, images);
        return "redirect:/Recruiter/JobManage";
    }

    @PostMapping("/closeJobPosting")
    @ResponseBody
    public String closeJobPosting(@RequestParam Long datanum, @RequestParam String region) {
        try { js.closeJobPosting(datanum, region); return "success"; } catch (Exception e) { return "fail"; }
    }
}
