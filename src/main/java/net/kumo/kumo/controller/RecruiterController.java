package net.kumo.kumo.controller;

import java.io.File;
import java.security.Principal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.kumo.kumo.domain.dto.JobApplicantGroupDTO;
import net.kumo.kumo.domain.dto.JobManageListDTO;
import net.kumo.kumo.domain.dto.JobPostingRequestDTO;
import net.kumo.kumo.domain.dto.JoinRecruiterDTO;
import net.kumo.kumo.domain.dto.ResumeResponseDTO;
import net.kumo.kumo.domain.entity.CompanyEntity;
import net.kumo.kumo.domain.entity.UserEntity;
import net.kumo.kumo.domain.entity.Enum.ApplicationStatus;
import net.kumo.kumo.repository.SeekerProfileRepository;
import net.kumo.kumo.repository.UserRepository;
import net.kumo.kumo.security.AuthenticatedUser;
import net.kumo.kumo.service.CompanyService;
import net.kumo.kumo.service.JobPostingService;
import net.kumo.kumo.service.RecruiterService;

/**
 * 구인자(Recruiter) 권한을 가진 사용자의 대시보드, 공고 관리, 지원자 관리,
 * 스카우트, 마이페이지 등의 기능을 처리하는 Controller 클래스입니다.
 */
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

    @Autowired
    private MessageSource messageSource;

    /**
     * 구인자 대시보드 메인 화면을 렌더링하며, 미확인 지원자 수 및 통계 데이터를 조회하여 전달합니다.
     *
     * @param model     뷰에 전달할 Model 객체
     * @param principal 현재 인증된 사용자 정보
     * @param user      Security Context의 인증 객체
     * @return 대시보드 메인 뷰 파일명
     */
    @GetMapping("Main")
    public String main(Model model, Principal principal, @AuthenticationPrincipal AuthenticatedUser user) {
        String userEmail = principal.getName();

        long unreadCount = rs.getUnreadCount(userEmail);
        net.kumo.kumo.domain.dto.RecruiterDashboardDTO stats = rs.getDashboardStats(userEmail);

        model.addAttribute("totalApplicants", stats.getTotalApplicants());
        model.addAttribute("unreadApplicants", unreadCount);
        model.addAttribute("todayVisits", stats.getTotalVisits());
        model.addAttribute("chartLabels", stats.getChartLabels());
        model.addAttribute("chartData", stats.getChartData());

        model.addAttribute("user", ur.findByEmail(userEmail).orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다.")));
        model.addAttribute("jobList", js.getMyJobPostings(userEmail));
        model.addAttribute("talents", rs.getScoutedProfiles());
        model.addAttribute("currentMenu", "home");

        return "recruiterView/main";
    }

    /**
     * 스카우트한 인재(구직자) 목록 화면을 렌더링합니다.
     *
     * @param model     뷰에 전달할 Model 객체
     * @param principal 현재 인증된 사용자 정보
     * @return 인재 목록 뷰 파일명
     */
    @GetMapping("TalentList")
    public String talentList(Model model, Principal principal) {
        UserEntity currentUser = ur.findByEmail(principal.getName()).orElseThrow();
        model.addAttribute("user", currentUser);
        model.addAttribute("talents", rs.getScoutedProfiles());
        model.addAttribute("currentMenu", "none");

        return "recruiterView/talentList";
    }

    /**
     * 특정 인재의 상세 프로필 및 이력서 정보를 조회하여 렌더링합니다.
     *
     * @param userId    조회할 인재(구직자) 식별자
     * @param model     뷰에 전달할 Model 객체
     * @param principal 현재 인증된 사용자 정보
     * @return 인재 상세 뷰 파일명
     */
    @GetMapping("TalentDetail")
    public String talentDetail(@RequestParam("userId") Long userId, Model model, Principal principal) {
        UserEntity currentUser = ur.findByEmail(principal.getName()).orElseThrow();
        model.addAttribute("user", currentUser);
        model.addAttribute("talent", ur.findById(userId).orElseThrow());
        model.addAttribute("resume", rs.getTalentResume(userId));
        model.addAttribute("profile", seekerProfileRepo.findByUser_UserId(userId).orElse(null));
        model.addAttribute("currentMenu", "none");

        return "recruiterView/talentDetail";
    }

    /**
     * 특정 구직자에게 스카우트 제의(Offer)를 발송합니다.
     *
     * @param seekerId    제의를 받을 구직자 식별자
     * @param userDetails 현재 인증된 사용자 정보
     * @param rttr        리다이렉트 시 전달할 플래시 속성 객체
     * @param locale      클라이언트 로케일 정보
     * @return 인재 상세 페이지 리다이렉트 URL
     */
    @GetMapping("/sendOffer")
    public String sendOffer(@RequestParam("userId") Long seekerId,
                            @AuthenticationPrincipal UserDetails userDetails,
                            RedirectAttributes rttr,
                            Locale locale) {
        try {
            rs.sendScoutOffer(userDetails.getUsername(), seekerId);
            rttr.addFlashAttribute("successMsg", messageSource.getMessage("scout.offer.success", null, locale));
        } catch (Exception e) {
            rttr.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/Recruiter/TalentDetail?userId=" + seekerId;
    }

    /**
     * 공고별 지원자 목록 화면을 렌더링하며, 아코디언 UI 처리를 위한 그룹화된 데이터를 전달합니다.
     *
     * @param model     뷰에 전달할 Model 객체
     * @param principal 현재 인증된 사용자 정보
     * @param sortBy    정렬 기준 필드
     * @return 지원자 관리 뷰 파일명
     */
    @GetMapping("ApplicantInfo")
    public String applicantInfo(Model model, Principal principal,
                                @RequestParam(defaultValue = "applicantCount") String sortBy) {
        model.addAttribute("currentMenu", "applicants");

        if (principal == null) {
            return "redirect:/login";
        }

        String loginEmail = principal.getName();
        UserEntity user = ur.findByEmail(loginEmail)
                .orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없습니다."));

        List<JobApplicantGroupDTO> groupedList = js.getGroupedApplicantsForRecruiter(user, sortBy);

        model.addAttribute("groupedList", groupedList);
        model.addAttribute("sortBy", sortBy);

        return "recruiterView/applicantInfo";
    }

    /**
     * 구인자가 등록한 공고 목록을 조회하고 정렬 기준에 따라 화면에 전달합니다.
     * 마감된(CLOSED) 공고는 정렬 기준과 무관하게 하단에 배치됩니다.
     *
     * @param model     뷰에 전달할 Model 객체
     * @param principal 현재 인증된 사용자 정보
     * @param sortBy    정렬 기준 필드
     * @param sortDir   정렬 방향
     * @return 공고 관리 뷰 파일명
     */
    @GetMapping("JobManage")
    public String jobManage(Model model, Principal principal,
                            @RequestParam(defaultValue = "date") String sortBy,
                            @RequestParam(defaultValue = "desc") String sortDir) {

        model.addAttribute("currentMenu", "jobManage");

        String userEmail = principal.getName();
        List<JobManageListDTO> jobList = js.getMyJobPostings(userEmail);

        Comparator<JobManageListDTO> comparator = switch (sortBy) {
            case "title" -> Comparator.comparing(JobManageListDTO::getTitle, Comparator.nullsLast(String::compareTo));
            case "region" -> Comparator.comparing(JobManageListDTO::getRegionType, Comparator.nullsLast(String::compareTo));
            case "salary" -> Comparator.comparing(
                    dto -> {
                        try {
                            return Long.parseLong(dto.getWage().replaceAll("[^0-9]", ""));
                        } catch (Exception e) {
                            return 0L;
                        }
                    }, Comparator.nullsLast(Comparator.naturalOrder()));
            default -> Comparator.comparing(JobManageListDTO::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
        };

        if ("desc".equals(sortDir)) {
            comparator = comparator.reversed();
        }

        jobList.sort(comparator);

        Comparator<JobManageListDTO> finalComparator = Comparator
                .comparing((JobManageListDTO dto) -> "CLOSED".equals(dto.getStatus()) ? 1 : 0)
                .thenComparing(comparator);

        jobList.sort(finalComparator);

        model.addAttribute("jobList", jobList);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);

        return "recruiterView/jobManage";
    }

    /**
     * 구인자 전용 캘린더 화면을 렌더링합니다.
     *
     * @param model 뷰에 전달할 Model 객체
     * @return 캘린더 뷰 파일명
     */
    @GetMapping("Calendar")
    public String calender(Model model) {
        model.addAttribute("currentMenu", "calendar");
        return "recruiterView/calendar";
    }

    /**
     * 구인자 계정 설정 및 프로필 관리 페이지를 렌더링합니다.
     *
     * @param model     뷰에 전달할 Model 객체
     * @param principal 현재 인증된 사용자 정보
     * @return 설정 뷰 파일명
     */
    @GetMapping("/Settings")
    public String settings(Model model, Principal principal) {
        model.addAttribute("currentMenu", "settings");
        return "recruiterView/settings";
    }

    /**
     * 구인자의 프로필 이미지를 서버 로컬 디렉토리에 업로드하고, DB 경로를 갱신합니다.
     *
     * @param file      업로드할 이미지 파일
     * @param principal 현재 인증된 사용자 정보
     * @return 업로드된 파일의 웹 접근 경로를 포함한 ResponseEntity
     */
    @PostMapping("/UploadProfile")
    @ResponseBody
    public ResponseEntity<?> uploadProfile(@RequestParam("profileImage") MultipartFile file, Principal principal) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("파일이 존재하지 않습니다.");
            }

            String uploadDir = System.getProperty("user.home") + "/kumo_uploads/profiles/";
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            File dest = new File(uploadDir + fileName);
            file.transferTo(dest);

            String userEmail = principal.getName();
            String webPath = "/upload/profiles/" + fileName;

            String originalFileName = file.getOriginalFilename();
            String storedFileName = fileName;
            Long fileSize = file.getSize();

            rs.updateProfileImage(userEmail, webPath, originalFileName, storedFileName, fileSize);

            return ResponseEntity.ok().body(webPath);
        } catch (Exception e) {
            log.error("프로필 이미지 업로드 중 예외 발생", e);
            return ResponseEntity.status(500).body("업로드 실패: " + e.getMessage());
        }
    }

    /**
     * 지원자 상세 프로필 조회 페이지를 렌더링합니다.
     *
     * @param model 뷰에 전달할 Model 객체
     * @return 지원자 상세 보기 뷰 파일명
     */
    @GetMapping("ApplicantDetail")
    public String applicantDetail(Model model) {
        return "recruiterView/applicantDetail";
    }

    /**
     * 구인자 회원 정보 수정 폼 페이지를 렌더링합니다.
     *
     * @param model 뷰에 전달할 Model 객체
     * @return 회원 정보 수정 뷰 파일명
     */
    @GetMapping("/ProfileEdit")
    public String profileEditForm(Model model) {
        return "recruiterView/profileEdit";
    }

    /**
     * 구인자의 회원 정보를 갱신 처리하고 설정 페이지로 리다이렉트합니다.
     *
     * @param dto 갱신할 사용자 정보가 담긴 DTO
     * @return 설정 페이지 리다이렉트 URL
     */
    @PostMapping("/ProfileEdit")
    public String profileEditProcess(@ModelAttribute JoinRecruiterDTO dto) {
        log.info("회원정보 수정 요청 데이터 처리");
        rs.updateProfile(dto);
        return "redirect:/Recruiter/Settings";
    }

    /**
     * 신규 구인 공고 등록 폼 페이지를 렌더링합니다.
     * 현재 구인자가 등록해둔 회사 목록 데이터를 함께 전달합니다.
     *
     * @param model       뷰에 전달할 Model 객체
     * @param userDetails 현재 인증된 사용자 정보
     * @return 공고 등록 뷰 파일명
     */
    @GetMapping("/JobPosting")
    public String jobPostingPage(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        UserEntity user = ur.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없습니다."));

        List<CompanyEntity> companies = cs.getCompanyList(user);
        model.addAttribute("companies", companies);

        return "recruiterView/jobPosting";
    }

    /**
     * 작성된 신규 구인 공고 데이터 및 첨부 이미지를 저장합니다.
     *
     * @param dto         공고 폼 데이터가 담긴 DTO
     * @param images      첨부된 다중 이미지 파일 리스트
     * @param userDetails 현재 인증된 사용자 정보
     * @return 공고 관리 페이지 리다이렉트 URL
     */
    @PostMapping("/JobPosting")
    public String submitJobPosting(
            @ModelAttribute JobPostingRequestDTO dto,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal UserDetails userDetails) {

        UserEntity user = ur.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없습니다."));

        js.saveJobPosting(dto, images, user);
        return "redirect:/Recruiter/JobManage";
    }

    /**
     * 구인자가 작성한 특정 공고를 DB에서 완전 삭제 처리합니다.
     *
     * @param datanum   삭제할 공고의 데이터 번호(식별자)
     * @param region    삭제할 공고의 지역(출처)
     * @param principal 현재 인증된 사용자 정보
     * @return 처리 성공 여부 메시지를 포함한 ResponseEntity
     */
    @DeleteMapping("/api/recruiter/postings")
    public ResponseEntity<?> deletePosting(
            @RequestParam("datanum") Long datanum,
            @RequestParam("region") String region,
            Principal principal) {
        try {
            String userEmail = principal.getName();
            js.deleteMyJobPosting(datanum, region, userEmail);
            return ResponseEntity.ok().body("공고가 성공적으로 삭제되었습니다.");

        } catch (IllegalStateException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("삭제 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 기존 구인 공고 수정을 위한 폼 페이지를 렌더링합니다.
     *
     * @param id          수정할 공고의 식별자
     * @param region      수정할 공고의 지역(출처)
     * @param model       뷰에 전달할 Model 객체
     * @param userDetails 현재 인증된 사용자 정보
     * @return 공고 수정 뷰 파일명
     */
    @GetMapping("/editJobPosting")
    public String editJobPostingPage(
            @RequestParam("id") Long id,
            @RequestParam("region") String region,
            Model model,
            @AuthenticationPrincipal UserDetails userDetails) {

        JobPostingRequestDTO job = js.getJobPostingForEdit(id, region);
        UserEntity user = ur.findByEmail(userDetails.getUsername()).orElseThrow();
        List<CompanyEntity> companies = cs.getCompanyList(user);

        model.addAttribute("job", job);
        model.addAttribute("companies", companies);
        model.addAttribute("region", region);
        model.addAttribute("jobId", id);

        return "recruiterView/editJobPosting";
    }

    /**
     * 수정된 구인 공고 데이터를 저장 처리합니다.
     *
     * @param id     수정할 공고 식별자
     * @param region 수정할 공고 지역(출처)
     * @param dto    수정 폼 데이터가 담긴 DTO
     * @param images 신규 첨부된 이미지 파일 리스트
     * @return 공고 관리 페이지 리다이렉트 URL
     */
    @PostMapping("/editJobPosting")
    public String updateJobPosting(
            @RequestParam("id") Long id,
            @RequestParam("region") String region,
            @ModelAttribute JobPostingRequestDTO dto,
            @RequestParam(value = "images", required = false) List<MultipartFile> images) {

        js.updateJobPosting(id, region, dto, images);
        return "redirect:/Recruiter/JobManage";
    }

    /**
     * 특정 구인 공고의 모집 상태를 마감(CLOSED)으로 변경 처리합니다.
     * 화면 이동 없이 비동기 응답(String)만 반환합니다.
     *
     * @param datanum 마감할 공고 데이터 번호
     * @param region  마감할 공고 지역(출처)
     * @return 처리 성공 여부 문자열 (success / fail)
     */
    @PostMapping("/closeJobPosting")
    @ResponseBody
    public String closeJobPosting(@RequestParam Long datanum, @RequestParam String region) {
        try {
            js.closeJobPosting(datanum, region);
            return "success";
        } catch (Exception e) {
            log.error("공고 마감 처리 중 오류 발생", e);
            return "fail";
        }
    }

    /**
     * 지원자 관리 모달창에서 열람할 특정 구직자의 이력서 상세 데이터를 반환합니다.
     *
     * @param seekerId 열람할 구직자 식별자
     * @return 이력서 정보가 담긴 JSON 객체를 포함한 ResponseEntity
     */
    @GetMapping("/api/resume/{seekerId}")
    @ResponseBody
    public ResponseEntity<ResumeResponseDTO> getApplicantResumeApi(@PathVariable("seekerId") Long seekerId) {
        try {
            ResumeResponseDTO resumeData = js.getApplicantResumeData(seekerId);
            return ResponseEntity.ok(resumeData);
        } catch (Exception e) {
            log.error("이력서 조회 중 에러 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 특정 지원 내역(Application)의 합격/불합격 상태를 변경합니다.
     *
     * @param appId  변경할 지원 내역 식별자
     * @param status 변경할 상태 문자열 (PASSED, FAILED 등)
     * @return 처리 성공 여부를 포함한 ResponseEntity
     */
    @PostMapping("/api/application/{appId}/status")
    @ResponseBody
    public ResponseEntity<?> updateAppStatus(@PathVariable Long appId, @RequestParam String status) {
        try {
            ApplicationStatus appStatus = ApplicationStatus.valueOf(status);
            js.updateApplicationStatus(appId, appStatus);
            return ResponseEntity.ok("success");
        } catch (Exception e) {
            log.error("지원자 상태 업데이트 실패", e);
            return ResponseEntity.badRequest().body("상태 업데이트 실패: " + e.getMessage());
        }
    }
}