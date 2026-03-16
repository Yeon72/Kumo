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

/**
 * 관리자(Admin) 전용 페이지 렌더링 및 관리를 담당하는 Controller 클래스입니다.
 */
@Slf4j
@Controller
@RequestMapping("admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    /**
     * 어드민 대시보드 메인 페이지를 렌더링합니다.
     *
     * @param model Model 객체
     * @param lang  클라이언트 언어 설정
     * @return 대시보드 뷰 파일명
     */
    @GetMapping("/dashboard")
    public String dashboardPage(Model model,
                                @RequestParam(value = "lang", defaultValue = "ko") String lang) {
        model.addAttribute("adminName", "Administrator");
        model.addAttribute("lang", lang);
        return "adminView/admin_dashboard";
    }

    /**
     * 대시보드에 렌더링할 통계 데이터를 JSON 형식으로 반환합니다.
     *
     * @return AdminDashboardDTO 객체
     */
    @GetMapping("/data")
    @ResponseBody
    public AdminDashboardDTO getDashboardData() {
        return adminService.getDashboardData();
    }

    /**
     * 회원 관리 페이지를 렌더링하며, 전체 회원 목록 및 가입 대기 중인 구인자 목록을 페이징하여 전달합니다.
     *
     * @param model       Model 객체
     * @param lang        클라이언트 언어 설정
     * @param searchType  검색 조건 (예: 이름, 이메일 등)
     * @param keyword     검색어
     * @param role        사용자 권한 필터
     * @param status      사용자 상태 필터
     * @param page        전체 회원 목록 페이지 번호
     * @param size        전체 회원 목록 페이지 크기
     * @param pendingPage 승인 대기 목록 페이지 번호
     * @param pendingSize 승인 대기 목록 페이지 크기
     * @param tab         현재 활성화된 탭 정보
     * @return 회원 관리 뷰 파일명
     */
    @GetMapping("/user")
    public String userManagementPage(Model model,
                                     @RequestParam(value = "lang", defaultValue = "ko") String lang,
                                     @RequestParam(value = "searchType", required = false) String searchType,
                                     @RequestParam(value = "keyword", required = false) String keyword,
                                     @RequestParam(value = "role", required = false) String role,
                                     @RequestParam(value = "status", required = false) String status,
                                     @RequestParam(value = "page", defaultValue = "0") int page,
                                     @RequestParam(value = "size", defaultValue = "10") int size,
                                     @RequestParam(value = "pendingPage", defaultValue = "0") int pendingPage,
                                     @RequestParam(value = "pendingSize", defaultValue = "10") int pendingSize,
                                     @RequestParam(value = "tab", defaultValue = "all") String tab) {

        // 1. 전체 회원 관리 탭 처리
        Pageable pageable = PageRequest.of(page, size);
        Page<UserManageDTO> users = adminService.getAllUsers(lang, searchType, keyword, role, status, pageable);

        model.addAttribute("users", users);
        model.addAttribute("lang", lang);
        model.addAttribute("searchType", searchType);
        model.addAttribute("keyword", keyword);
        model.addAttribute("role", role);
        model.addAttribute("status", status);

        // 전체 회원 페이지네이션 연산
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

        // 2. 구인자 승인 대기 탭 처리
        Pageable pendingPageable = PageRequest.of(pendingPage, pendingSize);
        Page<UserManageDTO> pendingRecruiters = adminService.getAllUsers(lang, null, null, "RECRUITER", "INACTIVE", pendingPageable);

        model.addAttribute("pendingRecruiters", pendingRecruiters);
        model.addAttribute("activeTab", tab);

        return "adminView/admin_user";
    }

    /**
     * 사용자 통계 데이터를 JSON 형식으로 반환합니다.
     *
     * @return 사용자 통계 데이터 Map
     */
    @GetMapping("/user/stats")
    public ResponseEntity<Map<String, Object>> getUserStats() {
        Map<String, Object> stats = adminService.getUserStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * 공고 및 신고 관리 페이지를 렌더링하며, 데이터를 페이징하여 전달합니다.
     *
     * @param model      Model 객체
     * @param lang       클라이언트 언어 설정
     * @param searchType 검색 조건
     * @param keyword    검색어
     * @param status     공고 상태 필터
     * @param page       공고 목록 페이지 번호
     * @param size       공고 목록 페이지 크기
     * @param reportPage 신고 목록 페이지 번호
     * @param reportSize 신고 목록 페이지 크기
     * @param tab        현재 활성화된 탭 정보
     * @return 공고 관리 뷰 파일명
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

        int pageBlock = 5;

        // 1. 공고 탭 처리
        Pageable postPageable = PageRequest.of(page, size);
        Page<JobSummaryDTO> posts = adminService.getAllJobSummaries(lang, searchType, keyword, status, postPageable);
        model.addAttribute("posts", posts);

        int postTotalPages = posts.getTotalPages() == 0 ? 1 : posts.getTotalPages();
        int postCurrent = posts.getNumber() + 1;
        int startPage = Math.max(1, postCurrent - (pageBlock / 2));
        int endPage = Math.min(postTotalPages, startPage + pageBlock - 1);

        if (postTotalPages <= pageBlock) {
            startPage = 1;
            endPage = postTotalPages;
        } else if (endPage - startPage + 1 < pageBlock) {
            startPage = Math.max(1, endPage - pageBlock + 1);
        }

        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("totalPages", postTotalPages);

        // 2. 신고 탭 처리
        Pageable reportPageable = PageRequest.of(reportPage, reportSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ReportDTO> reports = adminService.getAllReports(lang, reportPageable);
        model.addAttribute("reports", reports);

        int reportTotalPages = reports.getTotalPages() == 0 ? 1 : reports.getTotalPages();
        int reportCurrent = reports.getNumber() + 1;
        int reportStartPage = Math.max(1, reportCurrent - (pageBlock / 2));
        int reportEndPage = Math.min(reportTotalPages, reportStartPage + pageBlock - 1);

        if (reportTotalPages <= pageBlock) {
            reportStartPage = 1;
            reportEndPage = reportTotalPages;
        } else if (reportEndPage - reportStartPage + 1 < pageBlock) {
            reportStartPage = Math.max(1, reportEndPage - pageBlock + 1);
        }

        model.addAttribute("reportStartPage", reportStartPage);
        model.addAttribute("reportEndPage", reportEndPage);
        model.addAttribute("reportTotalPages", reportTotalPages);

        // 3. 상태 유지 파라미터 전달
        model.addAttribute("lang", lang);
        model.addAttribute("searchType", searchType);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("activeTab", tab);

        return "adminView/admin_post";
    }

    /**
     * 회원의 권한(Role) 및 상태(Status)를 수정합니다.
     *
     * @param payload 수정할 회원의 식별자 및 변경 데이터
     * @return 성공 여부를 포함한 ResponseEntity
     */
    @PostMapping("/user/edit")
    @ResponseBody
    public ResponseEntity<String> editUser(@RequestBody Map<String, String> payload) {
        try {
            Long userId = Long.valueOf(payload.get("userId"));
            String role = payload.get("role");
            String status = payload.get("status");

            log.info("유저 정보 수정 요청 - ID: {}, Role: {}, Status: {}", userId, role, status);
            adminService.updateUserRoleAndStatus(userId, role, status);

            return ResponseEntity.ok("Success");
        } catch (Exception e) {
            log.error("유저 정보 수정 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Fail");
        }
    }

    /**
     * 회원을 시스템에서 삭제합니다.
     *
     * @param payload 삭제할 회원의 식별자
     * @return 성공 여부를 포함한 ResponseEntity
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
            log.error("유저 삭제 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Fail");
        }
    }

    /**
     * 단일 공고의 상태(Status)를 수정합니다.
     *
     * @param payload 수정할 공고의 데이터 소스, 식별자, 변경할 상태
     * @return 성공 여부를 포함한 ResponseEntity
     */
    @PostMapping("/post/edit")
    @ResponseBody
    public ResponseEntity<String> editPost(@RequestBody Map<String, String> payload) {
        try {
            String source = payload.get("source");
            Long id = Long.valueOf(payload.get("id"));
            String status = payload.get("status");

            log.info("공고 상태 수정 요청 - Source: {}, ID: {}, Status: {}", source, id, status);
            adminService.updatePostStatus(source, id, status);

            return ResponseEntity.ok("Success");
        } catch (Exception e) {
            log.error("공고 상태 수정 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Fail");
        }
    }

    /**
     * 선택된 다수의 공고를 일괄 삭제합니다.
     *
     * @param payload 삭제할 공고들의 식별자 리스트
     * @return 성공 여부를 포함한 ResponseEntity
     */
    @PostMapping("/post/delete")
    @ResponseBody
    public ResponseEntity<String> deletePosts(@RequestBody Map<String, List<String>> payload) {
        List<String> mixedIds = payload.get("ids");
        log.info("다중 공고 삭제 요청 - IDs: {}", mixedIds);

        adminService.deleteMixedPosts(mixedIds);
        return ResponseEntity.ok("Deleted successfully");
    }

    /**
     * 선택된 다수의 신고 내역을 일괄 삭제합니다.
     *
     * @param payload 삭제할 신고 내역의 식별자 리스트
     * @return 성공 여부를 포함한 ResponseEntity
     */
    @PostMapping("/report/delete")
    @ResponseBody
    public ResponseEntity<String> deleteReports(@RequestBody Map<String, List<Long>> payload) {
        List<Long> ids = payload.get("ids");
        log.info("다중 신고 내역 삭제 요청 - IDs: {}", ids);

        adminService.deleteReports(ids);
        return ResponseEntity.ok("Deleted successfully");
    }

    /**
     * 특정 신고 내역의 처리 상태를 수정합니다.
     *
     * @param payload 수정할 신고 내역의 식별자 및 변경할 상태
     * @return 성공 여부를 포함한 ResponseEntity
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
            log.error("신고 상태 수정 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Fail");
        }
    }

    /**
     * 관리자 활동 로그 및 시스템 로그 페이지를 렌더링합니다.
     *
     * @param model Model 객체
     * @param lang  클라이언트 언어 설정
     * @return 로그 관리 뷰 파일명
     */
    @GetMapping("/log")
    public String logPage(Model model,
                          @RequestParam(value = "lang", defaultValue = "ko") String lang) {

        log.info("어드민 로그 페이지 렌더링 요청");

        model.addAttribute("currentMenu", "log");
        model.addAttribute("lang", lang);
        model.addAttribute("adminName", "Administrator"); // TODO: 실제 인증 정보로 대체 필요
        model.addAttribute("loginLogs", adminService.getRecentLoginLogs());

        return "adminView/admin_log";
    }
}