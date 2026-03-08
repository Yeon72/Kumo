package net.kumo.kumo.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.kumo.kumo.domain.dto.AdminDashboardDTO;
import net.kumo.kumo.domain.dto.JobSummaryDTO;
import net.kumo.kumo.domain.dto.ReportDTO;
import net.kumo.kumo.domain.dto.UserManageDTO;
import net.kumo.kumo.service.AdminService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // 대시보드 페이지
    @GetMapping("/dashboard")
    public String dashboardPage(Model model,
                                @RequestParam(value = "lang", defaultValue = "ko") String lang) {
        model.addAttribute("adminName", "Administrator");
        model.addAttribute("lang", lang);
        return "adminView/admin_dashboard";
    }

    // 대시보드 데이터 (JSON)
    @GetMapping("/data")
    @ResponseBody
    public AdminDashboardDTO getDashboardData() {
        return adminService.getDashboardData();
    }

    @GetMapping("/user")
    public String userManagementPage(Model model,
                                     @RequestParam(value = "lang", defaultValue = "ko") String lang,
                                     @RequestParam(value = "searchType", required = false) String searchType,
                                     @RequestParam(value = "keyword", required = false) String keyword,
                                     @RequestParam(value = "role", required = false) String role,
                                     @RequestParam(value = "status", required = false) String status,
                                     @RequestParam(value = "page", defaultValue = "0") int page,
                                     @RequestParam(value = "size", defaultValue = "10") int size,
                                     // [추가] 승인 탭을 위한 페이징 및 활성 탭 파라미터
                                     @RequestParam(value = "pendingPage", defaultValue = "0") int pendingPage,
                                     @RequestParam(value = "pendingSize", defaultValue = "10") int pendingSize,
                                     @RequestParam(value = "tab", defaultValue = "all") String tab) {

        // ============================
        // 1. 전체 회원 관리 탭 처리
        // ============================
        Pageable pageable = PageRequest.of(page, size);
        Page<UserManageDTO> users = adminService.getAllUsers(lang, searchType, keyword, role, status, pageable);

        model.addAttribute("users", users);
        model.addAttribute("lang", lang);

        // 필터값 유지
        model.addAttribute("searchType", searchType);
        model.addAttribute("keyword", keyword);
        model.addAttribute("role", role);
        model.addAttribute("status", status);

        // 페이지네이션 로직 (users)
        int totalPages = users.getTotalPages();
        if (totalPages == 0) totalPages = 1;
        int pageBlock = 5;
        int current = users.getNumber() + 1;
        int startPage = Math.max(1, current - (pageBlock / 2));
        int endPage = Math.min(totalPages, startPage + pageBlock - 1);

        if (endPage - startPage + 1 < pageBlock && totalPages >= pageBlock) {
            startPage = Math.max(1, endPage - pageBlock + 1);
        }

        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("totalPages", totalPages);

        // ============================
        // 2. [추가] 구인자 승인 탭 처리
        // ============================
        // 기존 서비스 메서드 재활용: role="RECRUITER", status="INACTIVE"로 고정 검색
        Pageable pendingPageable = PageRequest.of(pendingPage, pendingSize);
        Page<UserManageDTO> pendingRecruiters = adminService.getAllUsers(lang, null, null, "RECRUITER", "INACTIVE", pendingPageable);

        model.addAttribute("pendingRecruiters", pendingRecruiters);

        // 현재 활성화된 탭 상태 유지
        model.addAttribute("activeTab", tab);

        return "adminView/admin_user";
    }

    @GetMapping("/user/stats")
    public ResponseEntity<Map<String, Object>> getUserStats() {
        // 서비스에서 Map을 받아와서 그대로 JSON으로 리턴
        Map<String, Object> stats = adminService.getUserStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * [VIEW] 공고 관리 페이지
     * URL: /admin/post
     */
    @GetMapping("/post")
    public String postManagementPage(Model model,
                                     @RequestParam(value = "lang", defaultValue = "ko") String lang,
                                     @RequestParam(value = "searchType", required = false) String searchType,
                                     @RequestParam(value = "keyword", required = false) String keyword,
                                     @RequestParam(value = "status", required = false) String status,
                                     @RequestParam(value = "page", defaultValue = "0") int page,
                                     @RequestParam(value = "size", defaultValue = "10") int size,
                                     @RequestParam(value = "reportPage", defaultValue = "0") int reportPage,
                                     @RequestParam(value = "reportSize", defaultValue = "10") int reportSize,
                                     @RequestParam(value = "tab", defaultValue = "all") String tab) {

        int pageBlock = 5; // 보여줄 버튼 개수 (공통 사용)

        // ============================
        // 1. 공고 탭 처리 (posts)
        // ============================
        Pageable postPageable = PageRequest.of(page, size);
        Page<JobSummaryDTO> posts = adminService.getAllJobSummaries(lang, searchType, keyword, status, postPageable);
        model.addAttribute("posts", posts);

        int postTotalPages = posts.getTotalPages() == 0 ? 1 : posts.getTotalPages();
        int postCurrent = posts.getNumber() + 1;

        // [수정된 계산 로직]
        int startPage = Math.max(1, postCurrent - (pageBlock / 2));
        int endPage = Math.min(postTotalPages, startPage + pageBlock - 1);

        // 만약 전체 페이지 수가 블록 크기(5)보다 작거나 같으면, 시작은 무조건 1, 끝은 무조건 전체 페이지 수로 맞춤
        if (postTotalPages <= pageBlock) {
            startPage = 1;
            endPage = postTotalPages;
        } else if (endPage - startPage + 1 < pageBlock) {
            // 뒤쪽 페이지에 도달해서 남은 버튼이 5개 미만일 때 앞쪽 버튼을 땡겨오는 로직
            startPage = Math.max(1, endPage - pageBlock + 1);
        }

        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("totalPages", postTotalPages);


        // ============================
        // 2. 신고 탭 처리 (reports)
        // ============================
        Pageable reportPageable = PageRequest.of(reportPage, reportSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ReportDTO> reports = adminService.getAllReports(lang, reportPageable);
        model.addAttribute("reports", reports);

        int reportTotalPages = reports.getTotalPages() == 0 ? 1 : reports.getTotalPages();
        int reportCurrent = reports.getNumber() + 1;

        // [수정된 계산 로직]
        int reportStartPage = Math.max(1, reportCurrent - (pageBlock / 2));
        int reportEndPage = Math.min(reportTotalPages, reportStartPage + pageBlock - 1);

        // 만약 전체 페이지 수가 블록 크기(5)보다 작거나 같으면, 시작은 무조건 1, 끝은 무조건 전체 페이지 수로 맞춤
        if (reportTotalPages <= pageBlock) {
            reportStartPage = 1;
            reportEndPage = reportTotalPages;
        } else if (reportEndPage - reportStartPage + 1 < pageBlock) {
            // 뒤쪽 페이지에 도달해서 남은 버튼이 5개 미만일 때 앞쪽 버튼을 땡겨오는 로직
            reportStartPage = Math.max(1, reportEndPage - pageBlock + 1);
        }

        model.addAttribute("reportStartPage", reportStartPage);
        model.addAttribute("reportEndPage", reportEndPage);
        model.addAttribute("reportTotalPages", reportTotalPages);


        // ============================
        // 3. 상태 유지 파라미터 전달
        // ============================
        model.addAttribute("lang", lang);
        model.addAttribute("searchType", searchType);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("activeTab", tab); // 활성화된 탭 기억

        return "adminView/admin_post";
    }

    /**
     * 회원 정보 수정 API (권한, 상태)
     * URL: /admin/user/edit
     */
    @PostMapping("/user/edit")
    @ResponseBody
    public ResponseEntity<String> editUser(@RequestBody Map<String, String> payload) {
        try {
            Long userId = Long.valueOf(payload.get("userId"));
            String role = payload.get("role");
            String status = payload.get("status");

            log.info("유저 수정 요청 - ID: {}, Role: {}, Status: {}", userId, role, status);
            adminService.updateUserRoleAndStatus(userId, role, status);

            return ResponseEntity.ok("Success");
        } catch (Exception e) {
            log.error("유저 수정 중 에러 발생: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Fail");
        }
    }

    /**
     * 회원 삭제 API
     * URL: /admin/user/delete
     */
    @PostMapping("/user/delete")
    @ResponseBody
    public ResponseEntity<String> deleteUser(@RequestBody Map<String, Long> payload) {
        try {
            Long userId = payload.get("userId");
            log.info("유저 삭제 요청 - ID: {}", userId);

            adminService.deleteUser(userId);
            return ResponseEntity.ok("Deleted");
        } catch (Exception e) {
            log.error("유저 삭제 중 에러 발생: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Fail");
        }
    }

    /**
     * 공고 상태 수정 API
     * URL: /admin/post/edit
     */
    @PostMapping("/post/edit")
    @ResponseBody
    public ResponseEntity<String> editPost(@RequestBody Map<String, String> payload) {
        try {
            String source = payload.get("source");
            Long id = Long.valueOf(payload.get("id"));
            String status = payload.get("status");

            log.info("공고 수정 요청 - Source: {}, ID: {}, Status: {}", source, id, status);

            // 서비스 호출
            adminService.updatePostStatus(source, id, status);

            return ResponseEntity.ok("Success");
        } catch (Exception e) {
            log.error("공고 수정 중 에러 발생: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Fail");
        }
    }

    /**
     * 공고 삭제 API
     * URL: /admin/post/delete
     */
    @PostMapping("/post/delete")
    @ResponseBody
    public ResponseEntity<String> deletePosts(@RequestBody Map<String, List<String>> payload) {
        List<String> mixedIds = payload.get("ids");
        log.info("삭제 요청 공고 목록: {}", mixedIds);

        adminService.deleteMixedPosts(mixedIds);

        return ResponseEntity.ok("Deleted successfully");
    }

    /**
     * 신고 내역 삭제 API
     * URL: /admin/report/delete
     */
    @PostMapping("/report/delete")
    @ResponseBody
    public ResponseEntity<String> deleteReports(@RequestBody Map<String, List<Long>> payload) {
        List<Long> ids = payload.get("ids");
        log.info("삭제 요청 신고 목록: {}", ids);

        adminService.deleteReports(ids);

        return ResponseEntity.ok("Deleted successfully");
    }

    /**
     * [새로 추가] 신고 상태 수정 API
     * URL: /admin/report/edit
     */
    @PostMapping("/report/edit")
    @ResponseBody
    public ResponseEntity<String> editReport(@RequestBody Map<String, String> payload) {
        try {
            Long id = Long.valueOf(payload.get("id"));
            String status = payload.get("status");

            log.info("신고 상태 수정 요청 - ID: {}, Status: {}", id, status);
            adminService.updateReportStatus(id, status);

            return ResponseEntity.ok("Success");
        } catch (Exception e) {
            log.error("신고 상태 수정 중 에러 발생: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Fail");
        }
    }
    
    // ==========================================
    // 로그 페이지 (DataOps)
    // URL: /admin/log
    // ==========================================
    @GetMapping("/log")
    public String logPage(Model model,
                          @RequestParam(value = "lang", defaultValue = "ko") String lang) {
        
        log.info("어드민 로그 페이지 요청");
        
        // 현재 메뉴 상태, 언어, 관리자 이름 전달
        model.addAttribute("currentMenu", "log"); // 사이드바 'active' 떡칠용
        model.addAttribute("lang", lang);
        model.addAttribute("adminName", "Administrator"); // 나중에 실제 세션값으로 변경
        
        // 로그인 이력 가져와서 전달
        model.addAttribute("loginLogs", adminService.getRecentLoginLogs());
        
        return "adminView/admin_log";
    }
}