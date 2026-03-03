package net.kumo.kumo.controller;

import java.io.File;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.kumo.kumo.domain.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.kumo.kumo.domain.entity.CompanyEntity;
import net.kumo.kumo.domain.entity.UserEntity;
import net.kumo.kumo.repository.UserRepository;
import net.kumo.kumo.service.CompanyService;
import net.kumo.kumo.service.JobPostingService;
import net.kumo.kumo.service.RecruiterService;

// 구인자 페이지 컨트롤러
@Slf4j
@RequiredArgsConstructor
@RequestMapping("Recruiter")
@Controller
public class RecruiterController {

    private final UserRepository ur;
    private final RecruiterService rs;

    private final CompanyService cs;
    private final JobPostingService js;

    /**
     * 메인 컨트롤러
     * 
     * @param model
     * @return
     */
    @GetMapping("Main")
    public String Main(Model model, java.security.Principal principal) {

        // 전체 공고 수 출력 용
        String userEmail = principal.getName();
        List<JobManageListDTO> jobList = js.getMyJobPostings(userEmail);
        model.addAttribute("jobList", jobList);

        model.addAttribute("currentMenu", "home"); // 사이드바 선택(홈 메뉴)
        return "recruiterView/main";
    }

    /**
     * 지원자 관리 컨트롤러
     */
    @GetMapping("ApplicantInfo")
    public String ApplicantInfo(Model model, Principal principal) {
        model.addAttribute("currentMenu", "applicants"); // 사이드바 활성화

        // 1. 로그인 유저 검증
        if (principal == null) {
            return "redirect:/login";
        }

        String loginEmail = principal.getName();
        UserEntity user = ur.findByEmail(loginEmail)
                .orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없습니다."));

        // 2. 🌟 방금 만든 Service 메서드 호출! (실제 DB 데이터 긁어오기)
        List<JobApplicantGroupDTO> groupedList = jobPostingService.getGroupedApplicantsForRecruiter(user);

        // 3. HTML(Thymeleaf)로 데이터 던져주기
        model.addAttribute("groupedList", groupedList);

        return "recruiterView/applicantInfo";
    }

    /**
     * 공고 관리 컨트롤러
     * 
     * @param model
     * @param principal
     * @return
     */
    @GetMapping("JobManage")
    public String JobManage(Model model, java.security.Principal principal) {
        model.addAttribute("currentMenu", "jobManage");

        // 🌟 1. 로그인한 사용자의 이메일(또는 ID)을 가져옵니다.
        // (Principal 대신 @AuthenticationPrincipal AuthenticatedUser user 를 쓰셔도 됩니다!)
        String userEmail = principal.getName();

        // 🌟 2. 서비스에서 통합된 최신 공고 리스트를 가져옵니다.
        List<JobManageListDTO> jobList = js.getMyJobPostings(userEmail);

        // 🌟 3. 화면(HTML)으로 리스트를 넘겨줍니다.
        model.addAttribute("jobList", jobList);

        return "recruiterView/jobManage";
    }

    /**
     * 캘린더 컨트롤러
     * 
     * @param model
     * @return
     */
    @GetMapping("Calendar")
    public String Calender(Model model) {
        model.addAttribute("currentMenu", "calendar"); // 사이드바 선택(캘린더)
        return "recruiterView/calendar";
    }

    /**
     * 내 계정(settings) 컨트롤러
     * 
     * @param model
     * @return
     */
    @GetMapping("/Settings")
    public String Settings(Model model, Principal principal) {
        model.addAttribute("currentMenu", "settings"); // 사이드바 선택(내 계정))
        return "recruiterView/settings";
    }

    /**
     * 설정 프로필 사진 업로드
     * 
     * @param file
     * @param principal
     * @return
     */
    @PostMapping("/UploadProfile")
    @ResponseBody
    public ResponseEntity<?> uploadProfile(@RequestParam("profileImage") MultipartFile file, Principal principal) {
        try {
            if (file.isEmpty())
                return ResponseEntity.badRequest().body("파일이 없습니다.");

            // [핵심 수정] 맥북의 사용자 홈 디렉토리(/Users/이름)를 기준으로 경로를 잡습니다.
            // 이렇게 하면 톰캣 임시 폴더와 섞이지 않습니다.
            String uploadDir = System.getProperty("user.home") + "/kumo_uploads/profiles/";

            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs(); // 폴더가 없으면 생성 (매우 중요!)
            }

            // 파일명 생성
            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();

            // [중요] 절대 경로를 사용해 새 파일 객체를 만듭니다.
            File dest = new File(uploadDir + fileName);

            // 파일 저장
            file.transferTo(dest);

            // DB에는 웹에서 접근 가능한 가상 경로를 저장합니다.
            String userEmail = principal.getName();
            String webPath = "/upload/profiles/" + fileName;

            // 🌟 [추가] DB가 간절히 원하는 3가지 정보 추가 추출!
            String originalFileName = file.getOriginalFilename(); // 원래 파일명 (예: myface.jpg)
            String storedFileName = fileName; // UUID 붙은 파일명
            Long fileSize = file.getSize(); // 파일 용량

            // 🌟 [수정] 서비스로 5가지 정보를 꽉꽉 채워서 보냅니다!
            rs.updateProfileImage(userEmail, webPath, originalFileName, storedFileName, fileSize);

            return ResponseEntity.ok().body(Map.of("success", true, "imageUrl", webPath));
        } catch (Exception e) {
            e.printStackTrace(); // 콘솔에 상세 에러 출력
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 지원자 상세보기 컨트롤러
     * 
     * @param model
     * @return
     */
    @GetMapping("ApplicantDetail")
    public String ApplicantDetail(Model model) {
        return "recruiterView/applicantDetail";
    }

    /**
     * 회원정보 수정 컨트롤러
     * 
     * @param model
     * @return
     */
    @GetMapping("/ProfileEdit") // 습관적으로 앞에 슬래시(/)를 붙여주시면 라우팅 꼬임을 방지할 수 있습니다.
    public String ProfileEdit(Model model) {
    
		return "recruiterView/profileEdit";
    }

    /**
     * 회원정보 수정 요청
     * 
     * @return
     */
    @PostMapping("/ProfileEdit")
    public String ProfileEdit(@ModelAttribute JoinRecruiterDTO dto) {

        // TODO: rs.updateProfile(...) 같은 서비스 로직을 호출해서 DB를 수정합니다.
        log.info("회원정보 수정 요청 들어옴!");

        log.info("dto 받아온거 :{}", dto);
        rs.updateProfile(dto);

        // 수정이 완료되면 다시 설정 페이지나 메인 화면으로 돌려보냅니다. (새로고침 방지용 redirect 필수!)
        return "redirect:/Recruiter/Settings";
    }

    /**
     * 1. 필드에 JobPostingService 주입 추가
     */
    @Autowired // 또는 생성자 주입 방식으로
    private JobPostingService jobPostingService;

    /**
     * GET - 공고 등록 페이지
     */
    @GetMapping("/JobPosting")
    public String jobPostingPage(Model model,
            @AuthenticationPrincipal UserDetails userDetails) {

        // 1. UserDetails에서 이메일(username)을 추출해 실제 DB의 UserEntity를 가져옵니다.
        UserEntity user = ur.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없습니다."));

        // 2. 사장님이 등록한 회사 리스트 조회
        List<CompanyEntity> companies = cs.getCompanyList(user);

        model.addAttribute("companies", companies);
        return "recruiterView/jobPosting";
    }

    /**
     * POST - 공고 등록 처리
     */
    @PostMapping("/JobPosting")
    public String submitJobPosting(
            @ModelAttribute JobPostingRequestDTO dto,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal UserDetails userDetails) { // 🌟 누가 등록하는지 확인

        // 1. 현재 로그인한 사용자의 엔티티를 가져옵니다.
        UserEntity user = ur.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없습니다."));

        // 2. 서비스에 'user' 객체까지 전달합니다.
        js.saveJobPosting(dto, images, user);

        return "redirect:/Recruiter/JobManage";
    }

    /**
     * 공고 삭제 API
     */
    @DeleteMapping("/api/recruiter/postings")
    public ResponseEntity<?> deletePosting(@RequestParam("datanum") Long datanum,
            @RequestParam("region") String region,
            java.security.Principal principal) {
        try {
            // 로그인한 유저 이메일 가져오기
            String userEmail = principal.getName();

            // 삭제 서비스 호출
            jobPostingService.deleteMyJobPosting(datanum, region, userEmail);

            return ResponseEntity.ok().body("공고가 성공적으로 삭제되었습니다.");

        } catch (IllegalStateException e) {
            // 권한이 없을 때 (403 Forbidden)
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (Exception e) {
            // 기타 서버 에러 (500)
            return ResponseEntity.status(500).body("삭제 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 공고 수정 페이지
     */
    @GetMapping("/editJobPosting")
    public String editJobPostingPage(@RequestParam("id") Long id,
            @RequestParam("region") String region,
            Model model,
            @AuthenticationPrincipal UserDetails userDetails) {

        JobPostingRequestDTO job = js.getJobPostingForEdit(id, region);
        UserEntity user = ur.findByEmail(userDetails.getUsername()).get();
        List<CompanyEntity> companies = cs.getCompanyList(user);

        model.addAttribute("job", job);
        model.addAttribute("companies", companies);
        model.addAttribute("region", region);
        model.addAttribute("jobId", id);

        return "recruiterView/editJobPosting";
    }

    /**
     * 공고 수정 처리
     */
    @PostMapping("/editJobPosting")
    public String updateJobPosting(@RequestParam("id") Long id,
            @RequestParam("region") String region,
            @ModelAttribute JobPostingRequestDTO dto,
            @RequestParam(value = "images", required = false) List<MultipartFile> images) {

        js.updateJobPosting(id, region, dto, images);
        return "redirect:/Recruiter/JobManage";
    }

    /**
     * 공고 마감
     * 
     * @param datanum
     * @param region
     * @return
     */
    @PostMapping("/closeJobPosting")
    @ResponseBody // 🌟 화면 이동 없이 결과만 알려주기 위해 필요!
    public String closeJobPosting(@RequestParam Long datanum, @RequestParam String region) {
        try {
            jobPostingService.closeJobPosting(datanum, region);
            return "success";
        } catch (Exception e) {
            return "fail";
        }
    }
}