package net.kumo.kumo.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.kumo.kumo.domain.dto.ApplicationDTO;
import net.kumo.kumo.domain.dto.JobDetailDTO;
import net.kumo.kumo.domain.dto.JobSummaryDTO;
import net.kumo.kumo.domain.dto.ReportDTO;
import net.kumo.kumo.domain.entity.Enum;
import net.kumo.kumo.domain.entity.UserEntity;
import net.kumo.kumo.repository.UserRepository;
import net.kumo.kumo.service.MapService;
import net.kumo.kumo.service.ScrapService;

/**
 * 지도 뷰(View) 라우팅 및 구인 공고 데이터, 신고, 지원 기능 등을 처리하는 Controller 클래스입니다.
 */
@Slf4j
@Controller
@RequestMapping("map")
@RequiredArgsConstructor
public class MapController {

    @Value("${GOOGLE_MAPS_KEY}")
    private String googleMapKey;

    private final MapService mapService;
    private final ScrapService scrapService;
    private final UserRepository userRepo;

    /**
     * 메인 지도 페이지를 렌더링합니다.
     *
     * @param model 뷰에 전달할 데이터를 담는 Model 객체
     * @return 메인 지도 뷰 파일명
     */
    @GetMapping("main")
    public String mainMap(Model model) {
        log.debug("지도 메인 화면 렌더링 요청");
        model.addAttribute("googleMapsKey", googleMapKey);
        return "mainView/main";
    }

    /**
     * 구인 공고 리스트(모달/사이드바) 뷰를 반환합니다.
     *
     * @return 구인 리스트 뷰 파일명
     */
    @GetMapping("/job-list-view")
    public String jobListPage() {
        return "mapView/job_list";
    }

    /**
     * 특정 공고의 상세 페이지를 렌더링합니다.
     * 조회된 공고 작성자 본인 여부 및 스크랩(찜) 상태를 확인하여 뷰에 전달합니다.
     *
     * @param id        조회할 공고 식별자
     * @param source    공고 데이터 출처 (OSAKA, TOKYO 등)
     * @param lang      클라이언트 언어 설정
     * @param principal 현재 인증된 사용자 정보
     * @param model     뷰에 전달할 데이터를 담는 Model 객체
     * @return 공고 상세 페이지 뷰 파일명
     */
    @GetMapping("/jobs/detail")
    public String jobDetailPage(
            @RequestParam Long id,
            @RequestParam String source,
            @RequestParam(defaultValue = "kr") String lang,
            Principal principal,
            Model model) {

        JobDetailDTO job = mapService.getJobDetail(id, source, lang);
        boolean isOwner = false;
        boolean isSeeker = false;
        boolean isScraped = false;
        UserEntity user;

        if (principal != null) {
            String loginEmail = principal.getName();
            user = userRepo.findByEmail(loginEmail).orElse(null);

            if (user != null) {
                isScraped = scrapService.checkIsScraped(user.getUserId(), id, source);

                if (job.getUserId() != null) {
                    isOwner = user.getUserId().equals(job.getUserId());
                }
                isSeeker = (user.getRole() == Enum.UserRole.SEEKER);
            }
        }

        model.addAttribute("isScraped", isScraped);
        model.addAttribute("job", job);
        model.addAttribute("googleMapsKey", googleMapKey);
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("isSeeker", isSeeker);
        model.addAttribute("lang", lang);

        return "mapView/job_detail";
    }

    /**
     * 구직자가 특정 구인 공고에 지원(구인 신청)합니다.
     *
     * @param dto       지원 대상 공고 식별자 및 출처 정보가 담긴 DTO
     * @param principal 현재 인증된 사용자 정보
     * @return 처리 결과 메시지를 포함한 ResponseEntity
     */
    @PostMapping("/api/apply")
    @ResponseBody
    public ResponseEntity<String> applyForJob(@RequestBody ApplicationDTO.ApplyRequest dto, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }

        String loginEmail = principal.getName();
        UserEntity user = userRepo.findByEmail(loginEmail).orElse(null);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 사용자입니다.");
        }

        if (user.getRole() != Enum.UserRole.SEEKER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("구직자(SEEKER) 계정만 지원할 수 있습니다.");
        }

        try {
            mapService.applyForJob(user, dto);
            return ResponseEntity.ok("구인 신청이 완료되었습니다.");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("지원 처리 중 시스템 오류 발생: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("지원 처리 중 서버 오류가 발생했습니다.");
        }
    }

    /**
     * 구인자가 본인이 작성한 특정 공고를 삭제합니다.
     *
     * @param id        삭제할 공고 식별자
     * @param source    공고 데이터 출처
     * @param principal 현재 인증된 사용자 정보
     * @return 처리 결과 메시지를 포함한 ResponseEntity
     */
    @DeleteMapping("/api/jobs")
    @ResponseBody
    public ResponseEntity<String> deleteJob(
            @RequestParam Long id,
            @RequestParam String source,
            Principal principal) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }

        String loginEmail = principal.getName();
        UserEntity user = userRepo.findByEmail(loginEmail).orElse(null);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 사용자입니다.");
        }

        try {
            mapService.deleteJobPost(id, source, user);
            return ResponseEntity.ok("공고가 성공적으로 삭제되었습니다.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            log.error("공고 삭제 중 시스템 오류 발생: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("공고 삭제 중 오류가 발생했습니다.");
        }
    }

    /**
     * 상세 검색 리스트 페이지를 렌더링합니다.
     *
     * @return 검색 리스트 뷰 파일명
     */
    @GetMapping("/search_list")
    public String searchListPage() {
        return "mapView/search_job_list";
    }

    /**
     * 현재 지도 화면(Bounding Box) 영역 내에 존재하는 구인 공고 리스트를 조회합니다.
     *
     * @param minLat 최소 위도
     * @param maxLat 최대 위도
     * @param minLng 최소 경도
     * @param maxLng 최대 경도
     * @param lang   클라이언트 언어 설정
     * @return 구인 공고 요약 리스트(JobSummaryDTO)
     */
    @GetMapping("/api/jobs")
    @ResponseBody
    public List<JobSummaryDTO> getJobListApi(
            @RequestParam Double minLat,
            @RequestParam Double maxLat,
            @RequestParam Double minLng,
            @RequestParam Double maxLng,
            @RequestParam(defaultValue = "kr") String lang) {
        return mapService.getJobListInMap(minLat, maxLat, minLng, maxLng, lang);
    }

    /**
     * 특정 공고에 대한 사용자 신고를 접수합니다.
     *
     * @param reportDTO 신고 내용 및 대상 공고 정보가 담긴 DTO
     * @param principal 현재 인증된 사용자(신고자) 정보
     * @return 처리 결과 메시지를 포함한 ResponseEntity
     */
    @PostMapping("/api/reports")
    @ResponseBody
    public ResponseEntity<String> submitReport(@RequestBody ReportDTO reportDTO, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }

        String loginEmail = principal.getName();
        UserEntity user = userRepo.findByEmail(loginEmail).orElse(null);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 사용자입니다.");
        }

        reportDTO.setReporterId(user.getUserId());
        mapService.createReport(reportDTO);

        return ResponseEntity.ok("신고가 정상적으로 접수되었습니다.");
    }

    /**
     * 키워드 및 지역 조건을 기반으로 구인 공고를 검색합니다.
     *
     * @param keyword    검색 키워드 (선택)
     * @param mainRegion 메인 지역 필터 (선택)
     * @param subRegion  서브 지역 필터 (선택)
     * @param lang       클라이언트 언어 설정
     * @return 조건에 부합하는 공고 상세 리스트(JobDetailDTO)
     */
    @GetMapping("/api/jobs/search")
    @ResponseBody
    public List<JobDetailDTO> searchJobsApi(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String mainRegion,
            @RequestParam(required = false) String subRegion,
            @RequestParam(defaultValue = "kr") String lang) {
        log.info("조건 기반 공고 검색 API 호출 - Keyword: {}, MainRegion: {}, SubRegion: {}", keyword, mainRegion, subRegion);
        return mapService.searchJobsList(keyword, mainRegion, subRegion, lang);
    }
}